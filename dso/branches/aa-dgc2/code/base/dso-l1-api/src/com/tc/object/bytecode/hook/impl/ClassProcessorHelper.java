/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.net.NIOWorkarounds;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.ClassLoaderPreProcessorImpl;
import com.tc.object.bytecode.hook.ClassPostProcessor;
import com.tc.object.bytecode.hook.ClassPreProcessor;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.partitions.PartitionManager;
import com.tc.text.Banner;
import com.tc.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.LogManager;

/**
 * Helper class called by the modified version of java.lang.ClassLoader
 */
public class ClassProcessorHelper {

  /** Name reserved for apps running as root web app in a container */
  public static final String       ROOT_WEB_APP_NAME         = "ROOT";

  // Directory where Terracotta jars (and dependencies) can be found
  private static final String      TC_INSTALL_ROOT_SYSPROP   = "tc.install-root";

  // Property to indicate whether the Terracotta classloader is active
  private static final String      TC_ACTIVE_SYSPROP         = "tc.active";

  // NOTE: This is not intended to be a public/documented system property,
  // it is for dev use only. It is NOT for QA or customer use
  private static final String      TC_CLASSPATH_SYSPROP      = "tc.classpath";

  private static final String      TC_DSO_GLOBALMODE_SYSPROP = "tc.dso.globalmode";

  // Used for converting resource names into class names
  private static final String      CLASS_SUFFIX              = ".class";
  private static final int         CLASS_SUFFIX_LENGTH       = CLASS_SUFFIX.length();

  private static final boolean     GLOBAL_MODE_DEFAULT       = true;

  public static final boolean      USE_GLOBAL_CONTEXT;
  public static final boolean      USE_PARTITIONED_CONTEXT;

  private static final State       initState                 = new State();

  private static final String      tcInstallRootSysProp      = System.getProperty(TC_INSTALL_ROOT_SYSPROP);

  // This map should only hold a weak reference to the loader (key).
  // If we didn't we'd prevent loaders from being GC'd
  private static final Map         contextMap                = new WeakHashMap();
  private static final Map         partitionedContextMap     = new HashMap();

  private static URLClassLoader    tcLoader;
  private static DSOContext        globalContext;

  private static final boolean     TRACE;
  private static final PrintStream TRACE_STREAM;

  private static final String      PARTITIONED_MODE_SEP      = "#";

  private static volatile boolean  systemLoaderInitialized   = false;

  static {

    try {
      // eagerly load this class
      PartitionManager.init();

      // Make sure that the DSOContext class is loaded before using the
      // TC functionalities. This is needed for the IBM JDK when Hashtable is
      // instrumented for auto-locking in the bootjar.
      Class.forName(DSOContext.class.getName());

      String global = System.getProperty(TC_DSO_GLOBALMODE_SYSPROP, null);
      if (global != null) {
        USE_GLOBAL_CONTEXT = Boolean.valueOf(global).booleanValue();
      } else {
        USE_GLOBAL_CONTEXT = GLOBAL_MODE_DEFAULT;
      }

      USE_PARTITIONED_CONTEXT = inferPartionedMode();
      if (USE_PARTITIONED_CONTEXT && USE_GLOBAL_CONTEXT) {
        Banner.errorBanner("Both Global Context and Partitioned Context can not be enabled at the same time."
                           + "To use Partioned Context, you must set " + TC_DSO_GLOBALMODE_SYSPROP + "=false");
        Util.exit();
      }

      // See if we should trace or not -- if so grab System.[out|err] and keep a local reference to it. Applications
      // like WebSphere like to intercept this later and we can get caught in a loop and get a stack overflow
      final String traceOutput = System.getProperty("l1.classloader.trace.output", "none");
      PrintStream ts = null;
      if (traceOutput != null) {
        if ("stdout".equals(traceOutput)) {
          ts = System.out;
        } else if ("stderr".equals(traceOutput)) {
          ts = System.err;
        }
      }
      TRACE_STREAM = ts;
      TRACE = TRACE_STREAM != null;

    } catch (Throwable t) {
      Util.exit(t);
      throw new AssertionError(); // this has to be here to make the compiler happy
    }
  }

  private static URLClassLoader createTCLoader() throws Exception {
    return new URLClassLoader(buildTerracottaClassPath(), null);
  }

  private static boolean inferPartionedMode() {
    String tcConfig = System.getProperty(TVSConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME, null);
    if (tcConfig == null) return false;
    return tcConfig.indexOf(PARTITIONED_MODE_SEP) >= 0;
  }

  /**
   * Get resource URL
   * 
   * @param name Resource name
   * @param cl Loading classloader
   * @return URL to load resource from
   */
  public static URL getTCResource(String name, ClassLoader cl) {
    String className = null;
    if (name.endsWith(CLASS_SUFFIX)) {
      className = name.substring(0, name.length() - CLASS_SUFFIX_LENGTH).replace('/', '.');
    }

    URL resource = getClassResource(className, cl);

    if (null == resource) {
      if (!isAWRuntimeDependency(className)) { return null; }

      try {
        resource = tcLoader.findResource(name); // getResource() would cause an endless loop
      } catch (Exception e) {
        resource = null;
      }
    }

    return resource;
  }

  /**
   * Get TC class definition
   * 
   * @param name Class name
   * @param cl Classloader
   * @return Class bytes
   * @throws ClassNotFoundException If class not found
   */
  public static byte[] getTCClass(String name, ClassLoader cl) throws ClassNotFoundException {
    URL resource = getClassResource(name, cl);

    if (null == resource) {
      if (!isAWRuntimeDependency(name)) { return null; }

      resource = tcLoader.findResource(name.replace('.', '/') + ".class"); // getResource() would cause an endless loop
    }

    if (null == resource) { return null; }

    return getResourceBytes(resource);
  }

  private static byte[] getResourceBytes(URL url) throws ClassNotFoundException {
    InputStream is = null;
    try {
      is = url.openStream();
      byte[] b = new byte[is.available()];
      int len = 0;
      int n;
      while ((n = is.read(b, len, b.length - len)) > 0) {
        len += n;
        if (len < b.length) {
          byte[] c = new byte[b.length + 1000];
          System.arraycopy(b, 0, c, 0, len);
          b = c;
        }
      }
      if (len == b.length) { return b; }
      byte[] c = new byte[len];
      System.arraycopy(b, 0, c, 0, len);
      return c;
    } catch (Exception e) {
      throw new ClassNotFoundException("Unable to load " + url.toString() + "; " + e.toString(), e);
    } finally {
      try {
        is.close();
      } catch (Exception ex) {
        // ignore
      }
    }
  }

  private static URL getClassResource(String name, ClassLoader cl) {
    if (name != null) {
      DSOContext context = getContext(cl);
      if (context != null) { return context.getClassResource(name, cl); }
    }

    return null;
  }

  private static boolean isAWRuntimeDependency(String name) {
    if (null == name) { return false; }
    return name.startsWith("com.tcspring.");
    // || name.startsWith("com.tc.aspectwerkz.definition.deployer.AspectModule")
    // || name.equals("com.tc.aspectwerkz.aspect.AspectContainer")
    // || name.equals("com.tc.aspectwerkz.aspect.AbstractAspectContainer");
  }

  private static void handleError(Throwable t) {
    if (t instanceof InvocationTargetException) {
      t = ((InvocationTargetException) t).getTargetException();
    }

    if (t instanceof RuntimeException) { throw (RuntimeException) t; }
    if (t instanceof Error) { throw (Error) t; }

    throw new RuntimeException(t);
  }

  static File getTCInstallDir(boolean systemClassPathAllowed) {
    if (tcInstallRootSysProp == null) {
      if (systemClassPathAllowed) {
        try {
          ClassLoader.getSystemClassLoader().loadClass("com.tc.object.NotInBootJar");
          return null;
        } catch (ClassNotFoundException cnfe) {
          // ignore
        }
      }

      Banner.errorBanner("Terracotta home directory is not set. Please set it with -D" + TC_INSTALL_ROOT_SYSPROP
                         + "=<path-to-Terracotta-install>");
      Util.exit();
    }

    File tcInstallDir = new File(tcInstallRootSysProp);

    if (!tcInstallDir.exists() || !tcInstallDir.isDirectory() || !tcInstallDir.canRead()) {
      Banner.errorBanner("Terracotta install directory [" + tcInstallDir.getAbsolutePath()
                         + "] is not accessible. This value is set via system property " + TC_INSTALL_ROOT_SYSPROP);
      Util.exit();
    }

    return tcInstallDir;
  }

  private static URL[] buildTerracottaClassPath() throws Exception {
    if (System.getProperty(TC_CLASSPATH_SYSPROP) != null) { return buildDevClassPath(); }

    File tcHomeDir = getTCInstallDir(true);
    if (tcHomeDir == null) {
      ClassLoader classPathLoader = sun.misc.Launcher.getLauncher().getClassLoader();
      URL[] systemURLS = ((URLClassLoader) classPathLoader).getURLs();
      return systemURLS.clone();
    }

    File tcLib = new File(tcHomeDir, "lib");
    if (!tcLib.exists() || !tcLib.isDirectory() || !tcLib.canRead()) {
      Banner.errorBanner("Terracotta lib directory [" + tcLib.getAbsolutePath()
                         + "] is not accessible. This value is based off of the system property "
                         + TC_INSTALL_ROOT_SYSPROP);
      Util.exit();
    }

    File[] entries = tcLib.listFiles(new TcCommonLibQualifier());
    if (entries.length == 0) {
      Banner.errorBanner("Absolutely no .jar files or resources directory found in Terracotta common lib directory ["
                         + tcLib.getAbsolutePath() + "]. Please check the value of your " + TC_INSTALL_ROOT_SYSPROP
                         + " system property");
      Util.exit();
    }

    URL[] rv = new URL[entries.length];
    for (int i = 0; i < entries.length; i++) {
      String entry = entries[i].getCanonicalPath().replace(File.separatorChar, '/');
      if (entries[i].isDirectory()) entry += "/";
      rv[i] = new URL("file", "", entry);
    }

    return rv;
  }

  private static String slurpFile(String path) throws IOException {
    URL url = new URL(path);
    InputStream in = url.openStream();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    try {
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf, 0, buf.length)) >= 0) {
        bos.write(buf, 0, len);
      }
    } finally {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }

      try {
        bos.close();
      } catch (IOException ioe) {
        // ignore
      }
    }

    return new String(bos.toByteArray());
  }

  private static URL[] buildDevClassPath() throws MalformedURLException, IOException {
    // For development use only. This is handy since you can put the eclipse/ant build output directories
    // here and not bother creating a tc.jar every time you change some source

    String tcClasspath = System.getProperty(TC_CLASSPATH_SYSPROP);
    if (tcClasspath.startsWith("file:/")) {
      tcClasspath = slurpFile(tcClasspath);
    }

    String[] parts = tcClasspath.split(File.pathSeparator);
    ArrayList urls = new ArrayList();

    for (String part : parts) {
      if (part.length() > 0) {

        File file = new File(part);
        part = file.getAbsolutePath().replace(File.separatorChar, '/');

        if (!part.startsWith("/")) {
          part = "/" + part;
        }

        if (file.isDirectory()) {
          if (!part.endsWith("/")) {
            part = part + "/";
          }
        }

        urls.add(new URL("file", "", part));
      }
    }

    return (URL[]) urls.toArray(new URL[urls.size()]);
  }

  private static Method getContextMethod(String name, Class[] args) throws ClassNotFoundException,
      NoSuchMethodException {
    Class c = tcLoader.loadClass("com.tc.object.bytecode.hook.impl.DSOContextImpl");
    return c.getDeclaredMethod(name, args);
  }

  public static void initialize() {
    if (initState.attemptInit()) {
      try {
        tcLoader = createTCLoader();

        // do this before doing anything with the TC loader
        initTCLogging();

        if (USE_GLOBAL_CONTEXT) {
          globalContext = createGlobalContext();
        }

        if (USE_PARTITIONED_CONTEXT) {
          initParitionedContexts();
        }
        initState.initialized();

        System.setProperty(TC_ACTIVE_SYSPROP, Boolean.TRUE.toString());
      } catch (Throwable t) {
        t.printStackTrace();
        handleError(t);
        throw new AssertionError(); // shouldn't get here
      }
    }
  }

  private static void initParitionedContexts() throws Exception {
    String tcConfig = System.getProperty(TVSConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME);

    Assert.assertNotNull(tcConfig);
    Assert.assertTrue(tcConfig, tcConfig.indexOf(PARTITIONED_MODE_SEP) >= 0);

    String partitionedConfigSpecs[] = tcConfig.split(PARTITIONED_MODE_SEP);
    for (int i = 0; i < partitionedConfigSpecs.length; i++) {
      Method m = getContextMethod("createContext", new Class[] { String.class });
      DSOContext context = (DSOContext) m.invoke(null, new Object[] { partitionedConfigSpecs[i] });
      context.getManager().init();
      synchronized (partitionedContextMap) {
        partitionedContextMap.put("Partition" + i, context);
      }
    }
  }

  public static int getNumPartitions() {
    if (USE_PARTITIONED_CONTEXT) {
      synchronized (partitionedContextMap) {
        return partitionedContextMap.size();
      }
    }

    return 1;
  }

  private static void initTCLogging() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    // This code is here because users can set various Log4J properties that will, for example, cause Log4J to try
    // to use arbitrary classes as appenders. If users try to use one of their own classes as an appender, we'll try
    // to load it in our classloader and fail in fairly unpleasant ways.
    //
    // Yes, saving and restoring a system property really sucks, but there isn't really a better way to do it. Users
    // can request that Log4J read config from an arbitrary URL otherwise, and there's no way to intercept that at
    // all. As a result, this seems like a better solution.
    //
    // See LKC-1974 for more details.

    String oldDefaultInitOverrideValue = null;

    try {
      oldDefaultInitOverrideValue = System.setProperty("log4j.defaultInitOverride", "true");
      Class loggerClass = tcLoader.loadClass("org.apache.log4j.Logger");
      Method theMethod = loggerClass.getDeclaredMethod("getRootLogger", new Class[0]);
      theMethod.invoke(null, (Object[]) null);
    } finally {
      if (oldDefaultInitOverrideValue == null) {
        System.getProperties().remove("log4j.defaultInitOverride");
      } else {
        System.setProperty("log4j.defaultInitOverride", oldDefaultInitOverrideValue);
      }
    }
  }
  
  /**
   * @deprecated here so that old code is not broken. New classloader adapters should be
   * registered with {@link #registerGlobalLoader(NamedClassLoader, String)} to support
   * classloader app-group substitution.
   */
  public static void registerGlobalLoader(NamedClassLoader loader) {
    registerGlobalLoader(loader, null);
  }

  /**
   * Register a named classloader. If using a partitioned context, register the loader in all partitions.
   * @param webAppName the name of a web application that this is the loader for; or null if this is not
   * a web application classloader.
   */
  public static void registerGlobalLoader(NamedClassLoader loader, String webAppName) {
    if (!USE_GLOBAL_CONTEXT && !USE_PARTITIONED_CONTEXT) { throw new IllegalStateException(
                                                                                           "Not global/partitioned DSO mode"); }
    if (TRACE) traceNamedLoader(loader);

    if (USE_PARTITIONED_CONTEXT) {
      for (Iterator i = partitionedContextMap.values().iterator(); i.hasNext();) {
        DSOContext context = (DSOContext) i.next();
        context.getManager().registerNamedLoader(loader, webAppName);
      }
    } else {
      ManagerUtil.registerNamedLoader(loader, webAppName);
    }
  }

  /**
   * Shut down the ClassProcessorHelper
   */
  public static void shutdown() {
    if (USE_PARTITIONED_CONTEXT) {
      Manager[] managers = getPartitionedManagers();
      for (Manager manager : managers) {
        try {
          manager.stop();
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
      return;
    }
    if (!USE_GLOBAL_CONTEXT) { throw new IllegalStateException("Not global/partitioned DSO mode"); }
    try {
      if (globalContext != null) {
        globalContext.getManager().stop();
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
  
  /**
   * Given a context path, trim and condition it to be usable by methods 
   * such as {@link #isDSOSessions(String)}
   * @param context a servlet context path, as from HttpServletContext#getPath();
   * null, "", "/", or "//" will be interpreted as ROOT context.
   * @return a non-null, non-empty string
   */
  public static String computeAppName(String context) {
    // compute app name
    // deal with possible app strings: null, "", "/", "/xyz", "xyz/", "/xyz/"
    if (context == null) {
      return ROOT_WEB_APP_NAME;
    }
    context = context.trim();
    if (context.startsWith("/")) {
      context = context.substring(1);
    }
    if (context.endsWith("/")) {
      context = context.substring(0, context.length() - 2);
    }
    if (context.length() == 0) {
      return ROOT_WEB_APP_NAME;
    }
    return context;
  }

  /**
   * Check whether this web app is using DSO sessions
   * 
   * @param appName Web app name
   * @return True if DSO sessions enabled
   */
  public static boolean isDSOSessions(String appName) {
    if (USE_PARTITIONED_CONTEXT) return false;

    appName = ("/".equals(appName)) ? ROOT_WEB_APP_NAME : appName;
    try {
      Method m = getContextMethod("isDSOSessions", new Class[] { String.class });
      boolean rv = ((Boolean) m.invoke(null, new Object[] { appName })).booleanValue();
      return rv;
    } catch (Throwable t) {
      handleError(t);
      throw new AssertionError(); // shouldn't get here
    }
  }

  /**
   * WARNING: Used by test framework only
   * 
   * @param loader Loader
   * @param context DSOContext
   */
  public static void setContext(ClassLoader loader, DSOContext context) {
    if (USE_GLOBAL_CONTEXT || USE_PARTITIONED_CONTEXT) { throw new IllegalStateException(
                                                                                         "DSO Context is global/Partitioned in this VM"); }

    if ((loader == null) || (context == null)) {
      // bad dog
      throw new IllegalArgumentException("Loader and/or context may not be null");
    }

    synchronized (contextMap) {
      contextMap.put(loader, context);
    }
  }

  /**
   * WARNING: used by test framework only
   */
  public static Manager getManager(ClassLoader caller) {
    if (USE_GLOBAL_CONTEXT) { return globalContext.getManager(); }

    DSOContext context;
    synchronized (contextMap) {
      context = (DSOContext) contextMap.get(caller);
    }
    if (context == null) { return null; }
    return context.getManager();
  }

  public static Manager[] getPartitionedManagers() {
    if (!USE_PARTITIONED_CONTEXT) { return new Manager[] { ManagerUtil.getManager() }; }

    final DSOContext[] contexts;
    synchronized (partitionedContextMap) {
      contexts = (DSOContext[]) partitionedContextMap.values().toArray(new DSOContext[partitionedContextMap.size()]);
    }

    Manager[] managers = new Manager[contexts.length];
    for (int i = 0; i < contexts.length; i++) {
      managers[i] = contexts[i].getManager();
    }

    return managers;
  }

  /**
   * Get the DSOContext for this classloader
   * 
   * @param cl Loader
   * @return Context
   */
  public static DSOContext getContext(ClassLoader cl) {
    if (USE_GLOBAL_CONTEXT) return globalContext;
    if (USE_PARTITIONED_CONTEXT) { return getFirstPartionedContext(); }

    synchronized (contextMap) {
      return (DSOContext) contextMap.get(cl);
    }
  }

  private static DSOContext createGlobalContext() {
    try {
      Method m = getContextMethod("createGlobalContext", new Class[] {});
      DSOContext context = (DSOContext) m.invoke(null, new Object[] {});
      context.getManager().init();
      return context;
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(-1);
      throw new AssertionError(); // shouldn't get here
    }
  }

  /**
   * byte code instrumentation of class loaded <br>
   * XXX::NOTE:: Do NOT optimize to return same input byte array if the class was instrumented (I can't imagine why we
   * would). Our instrumentation in java.lang.ClassLoader checks the returned byte array to see if the class is
   * instrumented or not to maintain the array offset.
   * 
   * @param caller Loader defining class
   * @param name Class name
   * @param b Data
   * @param off Offset into b
   * @param len Length of class data
   * @param pd Protection domain for class
   * @return Modified class array
   * @see ClassLoaderPreProcessorImpl
   */
  public static byte[] defineClass0Pre(ClassLoader caller, String name, byte[] b, int off, int len, ProtectionDomain pd) {
    if (skipClass(caller)) { return b; }

    // needed for JRockit
    name = (name != null) ? name.replace('/', '.') : null;

    if (TRACE) traceLookup(caller, name);

    if (isAWDependency(name)) { return b; }

    /*
     * This current initialization strategy has one slight shortcoming. JVMTI agents cannot attach while initialization
     * is in progress.
     */
    initialize();

    if (!initState.isInitialized()) { return b; }

    ManagerUtil.enable();

    ClassPreProcessor preProcessor = getPreProcessor(caller);
    if (preProcessor == null) { return b; }

    return preProcessor.preProcess(name, b, off, len, caller);
  }

  private static boolean skipClass(ClassLoader caller) {
    return (caller == tcLoader);
  }

  /**
   * Post process class during definition
   * 
   * @param clazz Class being defined
   * @param caller Classloader doing definition
   */
  public static void defineClass0Post(Class clazz, ClassLoader caller) {
    ClassPostProcessor postProcessor = getPostProcessor(caller);
    if (!initState.isInitialized()) { return; }

    if (skipClass(caller)) { return; }

    if (postProcessor == null) { return; }

    postProcessor.postProcess(clazz, caller);
  }

  /**
   * @return Global Manager
   */
  public static Manager getGlobalManager() {
    return globalContext.getManager();
  }

  private static ClassPreProcessor getPreProcessor(ClassLoader caller) {
    if (USE_GLOBAL_CONTEXT) { return globalContext; }
    if (USE_PARTITIONED_CONTEXT) { return getFirstPartionedContext(); }
    synchronized (contextMap) {
      return (ClassPreProcessor) contextMap.get(caller);
    }
  }

  private static DSOContext getFirstPartionedContext() {
    synchronized (partitionedContextMap) {
      Iterator iter = partitionedContextMap.values().iterator();
      return (DSOContext) (iter.hasNext() ? iter.next() : null);
    }
  }

  private static ClassPostProcessor getPostProcessor(ClassLoader caller) {
    if (USE_GLOBAL_CONTEXT) { return globalContext; }

    synchronized (contextMap) {
      return (ClassPostProcessor) contextMap.get(caller);
    }
  }

  /**
   * Check whether this is an AspectWerkz dependency
   * 
   * @param className Class name
   * @return True if AspectWerkz dependency
   */
  public static boolean isAWDependency(final String className) {
    return (className == null)
           || className.endsWith("_AWFactory")// TODO AVF refactor
           || className.endsWith(TransformationConstants.JOIN_POINT_CLASS_SUFFIX)
           || className.startsWith("com.tc.aspectwerkz.") || className.startsWith("com.tc.asm.")
           || className.startsWith("com.tc.jrexx.") || className.startsWith("org.dom4j.")
           || className.startsWith("org.xml.sax.") || className.startsWith("javax.xml.parsers.")
           || className.startsWith("sun.reflect.Generated"); // issue on J2SE 5 reflection - AW-245
  }

  /**
   * Get type of lock used by sessions
   * 
   * @param appName Web app context
   * @return Lock type
   */
  public static int getSessionLockType(String appName) {
    return globalContext.getSessionLockType(appName);
  }

  public static boolean isApplicationSessionLocked(String appName) {
    return globalContext.isApplicationSessionLocked(appName);
  }

  private static void traceNamedLoader(final NamedClassLoader ncl) {
    trace("loader[" + ncl + "] of type[" + ncl.getClass().getName() + "] registered as["
          + ncl.__tc_getClassLoaderName() + "]");
  }

  private static void traceLookup(final ClassLoader cl, final String clazz) {
    trace("loader[" + cl + "] of type[" + cl.getClass().getName() + "] looking for class[" + clazz + "]");
  }

  private static void trace(final String msg) {
    TRACE_STREAM.println("<TRACE> TC classloading: " + msg);
    TRACE_STREAM.flush();
  }

  public static void systemLoaderInitialized() {
    // This avoids a deadlock (see LKC-853, LKC-1387)
    java.security.Security.getProviders();

    // Avoid another deadlock (DEV-1047, DEV-1301)
    // Looking at the sun-jdk17 sources, this deadlock shouldn't happen there
    LogManager.getLogManager();

    // Workaround bug in NIO on solaris 10
    NIOWorkarounds.solaris10Workaround();

    systemLoaderInitialized = true;
  }

  /**
   * File filter for lib/*.jar files and lib/resources directory
   */
  public static class TcCommonLibQualifier implements FileFilter {
    public boolean accept(File pathname) {
      return (pathname.isDirectory() && pathname.getName().equals("resources"))
             || (pathname.isFile() && pathname.getAbsolutePath().toLowerCase().endsWith(".jar"));
    }
  }

  /**
   * ClassProcessorHelper initialization state
   */
  public static final class State {
    private static final int NOT_INITIALIZED = 0;
    private static final int INITIALIZING    = 1;
    private static final int INITIALIZED     = 2;

    private int              state           = NOT_INITIALIZED;

    final synchronized boolean attemptInit() {
      if ((state == NOT_INITIALIZED) && systemLoaderInitialized) {
        state = INITIALIZING;
        return true;
      }
      return false;
    }

    final synchronized void initialized() {
      if (state != INITIALIZING) { throw new IllegalStateException("State was " + state); }
      state = INITIALIZED;
    }

    final synchronized boolean isInitialized() {
      return state == INITIALIZED;
    }

  }

}
