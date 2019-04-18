/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.logging;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.PatternMatch;
import org.apache.logging.log4j.core.layout.ScriptPatternSelector;
import org.apache.logging.log4j.core.script.Script;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;


/**
 * Factory class for obtaining TCLogger instances.
 *
 * @author teck
 */
public class TCLogging {

  public static final String LOG_CONFIGURATION_PREFIX = "The configuration read for Logging: ";

  private static final int MAX_BUFFERED_LOG_MESSAGES = 10 * 1000;

  private static final String TERRACOTTA_L1_LOG_FILE_NAME = "terracotta-client.log";
  private static final String TERRACOTTA_L2_LOG_FILE_NAME = "terracotta-server.log";
  private static final String TERRACOTTA_GENERIC_LOG_FILE_NAME = "terracotta-generic.log";

  private static final String LOCK_FILE_NAME = ".terracotta-logging.lock";

  private static final String[] INTERNAL_LOGGER_NAMESPACES = new String[] { "com.tc", "com.terracotta",
      "com.terracottatech", "org.terracotta", "tc.operator" };

  private static final String CUSTOMER_LOGGER_NAMESPACE = "com.terracottatech";
  private static final String CUSTOMER_LOGGER_NAMESPACE_WITH_DOT = CUSTOMER_LOGGER_NAMESPACE + ".";

  private static final String CONSOLE_LOGGER_NAME = CUSTOMER_LOGGER_NAMESPACE + ".console";
  public static final String DUMP_LOGGER_NAME = "com.tc.dumper.dump";
  public static final String DERBY_LOGGER_NAME = "com.tc.derby.log";
  private static final String OPERATOR_EVENT_LOGGER_NAME = "tc.operator.event";

  private static final String LOGGING_PROPERTIES_SECTION = "logging";
  private static final String MAX_LOG_FILE_SIZE_PROPERTY = "maxLogFileSize";
  private static final int DEFAULT_MAX_LOG_FILE_SIZE = 512;
  private static final String MAX_BACKUPS_PROPERTY = "maxBackups";
  private static final int DEFAULT_MAX_BACKUPS = 20;
  private static final String LOG4J_CUSTOM_FILENAME = ".tc.custom.log4j.properties";
  public static final String LOG4J_PROPERTIES_FILENAME = ".tc.dev.log4j.properties";

  private static final String CONSOLE_PATTERN = "%d %p - %m%n";
  public static final String DUMP_PATTERN = "[dump] %m%n";
  public static final String DERBY_PATTERN = "[derby.log] %m%n";
  private static final String CONSOLE_PATTERN_DEVELOPMENT = "%d [%t] %p %c - %m%n";
  public static final String FILE_AND_JMX_PATTERN = "%d [%t] %p %c - %m%n";

  private static  TCLogger console;
  private static  TCLogger operatorEventLogger;
  private static Appender consoleAppender;

  private static  Logger[] allLoggers;

  private static DelegatingAppender delegateFileAppender;
  private static DelegatingAppender delegateBufferingAppender;
  private static boolean            buffering;
  private static File               currentLoggingDirectory = null;
  private static FileLock           currentLoggingDirectoryFileLock = null;
  private static boolean            lockingDisabled = false;

  private static Properties         loggingProperties;

  public static TCLogger getLogger(Class clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getLogger(clazz.getName());
  }

  public static TCLogger getLogger(String name) {
    if (name == null) { throw new NullPointerException("Logger cannot be null"); }

    boolean allowedName = false;
    for (String namespace : INTERNAL_LOGGER_NAMESPACES) {
      String withDot = namespace + ".";
      if (name.startsWith(withDot)) {
        allowedName = true;
        break;
      }
    }

    if (!allowedName) {
      //
      throw new IllegalArgumentException("Logger name (" + name + ") not in valid namespace: "
                                         + Arrays.asList(INTERNAL_LOGGER_NAMESPACES));
    }

    return new TCLoggerImpl(name);
  }

  /**
   * This method lets you get a logger w/o any name restrictions. FOR TESTS ONLY (ie. not for shipping code)
   */
  public static TCLogger getTestingLogger(String name) {
    if (name == null) { throw new IllegalArgumentException("Name cannot be null"); }
    return new TCLoggerImpl(name);
  }

  /**
   * This method lets you get a logger w/o any name restrictions. FOR TESTS ONLY (ie. not for shipping code)
   */
  public static TCLogger getTestingLogger(Class clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getTestingLogger(clazz.getName());
  }

  // You want to look at CustomerLogging to get customer facing logger instances
  static TCLogger getCustomerLogger(String name) {
    if (name == null) { throw new IllegalArgumentException("name cannot be null"); }

    name = CUSTOMER_LOGGER_NAMESPACE_WITH_DOT + name;

    if (CONSOLE_LOGGER_NAME.equals(name)) { throw new IllegalArgumentException("Illegal name: " + name); }

    return new TCLoggerImpl(name);
  }

  // this method not public on purpose, use CustomerLogging.getConsoleLogger() instead
  static TCLogger getConsoleLogger() {
    return console;
  }

  static TCLogger getOperatorEventLogger() {
    return operatorEventLogger;
  }

  private static void reportLoggingError(Exception e) {
    reportLoggingError(null, e);
  }

  private static void reportLoggingError(String message, Exception e) {
    String newLine = System.getProperty("line.separator");
    StringBuffer errorMsg = new StringBuffer(newLine);

    if (message != null) {
      errorMsg.append("WARN: ").append(message).append(newLine);
    }

    if (e != null) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      errorMsg.append(sw.toString());
    }

    System.err.println(errorMsg.toString());
  }

  private static boolean developmentConfiguration() {
    try {
      Properties devLoggingProperties = new Properties();

      // Specify the order of LEAST importance; last one in wins
      File[] devLoggingLocations = new File[] { new File(System.getProperty("user.home"), LOG4J_PROPERTIES_FILENAME),
          new File(System.getProperty("user.dir"), LOG4J_PROPERTIES_FILENAME) };

      boolean devLog4JPropsFilePresent = false;
      InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(LOG4J_PROPERTIES_FILENAME);
      if (stream != null) {
        System.setProperty("log4j.configurationFile", LOG4J_PROPERTIES_FILENAME);
        devLog4JPropsFilePresent = true;
        try {
          devLoggingProperties.load(stream);
        } finally {
          stream.close();
        }
      } else {
        for (File propFile : devLoggingLocations) {
          if (propFile.isFile() && propFile.canRead()) {
            System.setProperty("log4j.configurationFile", propFile.getAbsolutePath()); // This should do the job of PropertyConfigurator
            devLog4JPropsFilePresent = true;
            InputStream in = new FileInputStream(propFile);
            try {
              devLoggingProperties.load(in);
            } finally {
              in.close();
            }
          }
        }
      }
      if (devLog4JPropsFilePresent) {
        loggingProperties = devLoggingProperties;
        // If empty file is there log4j2 uses its default configuration to print logs on console
        // which will result in repetition.
        if(!devLoggingProperties.isEmpty()) {
          Configurator.setRootLevel(Level.INFO);
        } else {
          initializeLogging();
        }
        return true;
      }
    } catch (Exception e) {
      reportLoggingError(e);
    }

    return false;
  }


  private static boolean customConfiguration() {
    try {
      // First one wins:
      List<File> locations = new ArrayList<File>();
      if (System.getenv("TC_INSTALL_DIR") != null) {
        locations.add(new File(System.getenv("TC_INSTALL_DIR"), LOG4J_CUSTOM_FILENAME));
      }
      locations.add(new File(System.getProperty("user.home"), LOG4J_CUSTOM_FILENAME));
      locations.add(new File(System.getProperty("user.dir"), LOG4J_CUSTOM_FILENAME));

      for (File propFile : locations) {
        if (propFile.isFile() && propFile.canRead()) {
          System.setProperty("log4j.configurationFile", propFile.getAbsolutePath()); // This should do the job of PropertyConfigurator
          Properties properties = new Properties();
          FileInputStream fis = null;
          try {
            fis = new FileInputStream(propFile);
            properties.load(fis);
          } finally {
            IOUtils.closeQuietly(fis);
          }

          loggingProperties = properties;
          if(properties.isEmpty()) {
            initializeLogging();
          }
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      reportLoggingError(e);
      return false;
    }
  }

  /**
   * <strong>FOR TESTS ONLY</strong>. This allows tests to successfully blow away directories containing log files on
   * Windows. This is a bit of a hack, but working around it is otherwise an enormous pain &mdash; tests only fail on
   * Windows, and you must then very carefully go around, figure out exactly why, and then work around it. Use of this
   * method makes everything a great deal simpler.
   */
  public static synchronized void disableLocking() {
    lockingDisabled = true;

    if (currentLoggingDirectoryFileLock != null) {
      try {
        currentLoggingDirectoryFileLock.release();
        currentLoggingDirectoryFileLock.channel().close();
        currentLoggingDirectoryFileLock = null;
      } catch (IOException ioe) {
        throw Assert.failure("Unable to release file lock?", ioe);
      }
    }
  }

  public static final int PROCESS_TYPE_GENERIC = 0;
  public static final int PROCESS_TYPE_L1      = 1;
  public static final int PROCESS_TYPE_L2      = 2;

  public static void setLogDirectory(File theDirectory, int processType) {
    Assert.assertNotNull(theDirectory);

    if (theDirectory.getName().trim().equalsIgnoreCase("stdout:")
        || theDirectory.getName().trim().equalsIgnoreCase("stderr:")) {
      if (currentLoggingDirectory != null
          && currentLoggingDirectory.getName().trim().equalsIgnoreCase(theDirectory.getName())) {
        // Nothing to do; great!
        return;
      }

      delegateFileAppender.setDelegate(NullAppender.createAppender("null"));

      LoggerContext context = LoggerContext.getContext(false);
      context.getRootLogger().addAppender(consoleAppender);

      if (buffering) {
        BufferingAppender realBufferingAppender = (BufferingAppender) delegateBufferingAppender
            .setDelegate(NullAppender.createAppender("null"));
        realBufferingAppender.stopAndSendContentsTo(consoleAppender);
        realBufferingAppender.stop();
        buffering = false;
      }

      boolean stdout = theDirectory.getName().trim().equalsIgnoreCase("stdout:");
      getConsoleLogger().info("All logging information now output to standard " + (stdout ? "output" : "error") + ".");

      return;
    }

    synchronized (TCLogging.class) {
      if (currentLoggingDirectory != null) {
        try {
          if (theDirectory.getCanonicalPath().equals(currentLoggingDirectory.getCanonicalPath())) { return; }
        } catch (IOException ioe) {
          // oh, well -- what can we do? we'll continue on.
        }
      }
    }

    try {
      FileUtils.forceMkdir(theDirectory);
    } catch (IOException ioe) {
      reportLoggingError("We can't create the directory '" + theDirectory.getAbsolutePath()
                         + "' that you specified for your logs.", ioe);
      return;
    }

    if (!theDirectory.canWrite()) {
      // formatting
      reportLoggingError("The log directory, '" + theDirectory.getAbsolutePath() + "', can't be written to.", null);
      return;
    }

    FileLock thisDirectoryLock = null;

    if (!lockingDisabled) {
      File lockFile = new File(theDirectory, LOCK_FILE_NAME);

      try {
        lockFile.createNewFile();
        Assert.eval(lockFile.exists());
        FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
        thisDirectoryLock = channel.tryLock();

        if (thisDirectoryLock == null) {
          reportLoggingError("The log directory, '" + theDirectory.getAbsolutePath()
                             + "', is already in use by another "
                             + "Terracotta process. Logging will proceed to the console only.", null);
          return;
        }

      } catch (OverlappingFileLockException ofle) {
        // This VM already holds the lock; no problem
      } catch (IOException ioe) {
        reportLoggingError("We can't lock the file '" + lockFile.getAbsolutePath() + "', to make sure that only one "
                           + "Terracotta process is using this directory for logging. This may be a permission "
                           + "issue, or some unexpected error. Logging will proceed to the console only.", ioe);
        return;
      }
    }

    RollingFileAppender newFileAppender;

    String logFileName;

    switch (processType) {
      case PROCESS_TYPE_L1:
        logFileName = TERRACOTTA_L1_LOG_FILE_NAME;
        break;

      case PROCESS_TYPE_L2:
        logFileName = TERRACOTTA_L2_LOG_FILE_NAME;
        break;

      case PROCESS_TYPE_GENERIC:
        logFileName = TERRACOTTA_GENERIC_LOG_FILE_NAME;
        break;

      default:
        throw Assert.failure("Unknown process type: " + processType);
    }

    String logFilePath = new File(theDirectory, logFileName).getAbsolutePath();
    String fileNamePrefix = "";
    String fileNameSuffix = "";
    int index = logFilePath.lastIndexOf(".");
    fileNamePrefix = logFilePath.substring(0, index);
    fileNameSuffix = logFilePath.substring(index);
    synchronized (TCLogging.class) {
      try {
        TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(LOGGING_PROPERTIES_SECTION);
        int maxLogFileSize = props.getInt(MAX_LOG_FILE_SIZE_PROPERTY, DEFAULT_MAX_LOG_FILE_SIZE);

        SizeBasedTriggeringPolicy sizeBasedTriggeringPolicy = SizeBasedTriggeringPolicy.createPolicy(maxLogFileSize + "MB");

        int maxBackupIndex = props.getInt(MAX_BACKUPS_PROPERTY, DEFAULT_MAX_BACKUPS);
        RolloverStrategy rolloverStrategy = DefaultRolloverStrategy
            .newBuilder()
            .withMin("1")
            .withMax(String.valueOf(maxBackupIndex))
            .build();
        PatternMatch patternMatch[] = new PatternMatch[2];
        patternMatch[0] = PatternMatch.newBuilder().setKey(DUMP_PATTERN).setPattern(DUMP_PATTERN).build();
        patternMatch[1] = PatternMatch.newBuilder().setKey(DERBY_PATTERN).setPattern(DERBY_PATTERN).build();
        Script s = Script.createScript("selector", "JavaScript",
            "var result\n" +
            "switch (logEvent.getLoggerName())\n" +
            "                        {\n" +
            "                         case \"com.tc.dumper.dump\":result=\"[dump] %m%n\";break;\n" +
            "                         case \"com.tc.derby.log\":result=\"[derby.log] %m%n\";break;\n" +
            "                         default:result=null;\n" +
            "                         }\n" +
            "                         result;");

        ScriptPatternSelector scriptPatternSelector = ScriptPatternSelector.newBuilder()
            .setScript(s)
            .setProperties(patternMatch)
            .setDefaultPattern(FILE_AND_JMX_PATTERN)
            .setConfiguration(LoggerContext.getContext(false).getConfiguration())
            .build();
        PatternLayout layout = PatternLayout.newBuilder()
            .withPatternSelector(scriptPatternSelector)
            .build();
        newFileAppender = RollingFileAppender.newBuilder()
            .withLayout(layout)
            .withName("file appender")
            .withAppend(true)
            .withStrategy(rolloverStrategy)
            .withPolicy(sizeBasedTriggeringPolicy)
            .withFileName(logFilePath)
            .withFilePattern(fileNamePrefix + ".%i" + fileNameSuffix)
            .build();
        // This makes us start with a new file each time.
        newFileAppender.getManager().rollover();
        // Note: order of operations is very important here. We start the new appender before we close and remove the
        // old one so that you don't drop any log records.
        Appender oldFileAppender = delegateFileAppender.setDelegate(newFileAppender);

        if(oldFileAppender!= null) {
          oldFileAppender.stop();
        }

        if (buffering) {
          BufferingAppender realBufferingAppender = (BufferingAppender) delegateBufferingAppender
              .setDelegate(NullAppender.createAppender("null"));
          realBufferingAppender.stopAndSendContentsTo(delegateFileAppender);
          realBufferingAppender.stop();
          buffering = false;
        }

        currentLoggingDirectory = theDirectory;

        if (currentLoggingDirectoryFileLock != null) currentLoggingDirectoryFileLock.release();
        currentLoggingDirectoryFileLock = thisDirectoryLock;
      } catch (IOException ioe) {
        reportLoggingError("We were unable to switch the logging system to log to '" + logFilePath + "'.", ioe);
      }
    }

    getConsoleLogger().info("Log file: '" + logFilePath + "'.");
    writeSystemProperties();
  }

  public static TCLogger getDumpLogger() {
    return new TCLoggerImpl(DUMP_LOGGER_NAME);
  }

  public static TCLogger getDerbyLogger() {
    return new TCLoggerImpl(DERBY_LOGGER_NAME);
  }

  private static void initializeLogging() {
    ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
    builder.setConfigurationName("Terracotaa-Login");
    builder.add(builder.newRootLogger(Level.INFO));
    Configurator.initialize(builder.build());

    // For disabling the log4j2 shutdown hook to log information from our application's shutdownhook.
    final LoggerContextFactory factory = LogManager.getFactory();
    if (factory instanceof Log4jContextFactory) {
      Log4jContextFactory contextFactory = (Log4jContextFactory) factory;
      ((DefaultShutdownCallbackRegistry) contextFactory.getShutdownCallbackRegistry()).stop();
    }
  }
  static {
    ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(TCLogging.class.getClassLoader());

    try {
      boolean customLogging = customConfiguration();
      boolean isDev = customLogging ? false : developmentConfiguration();
      if(!customLogging && !isDev) {
        initializeLogging();
      }

      console = new TCLoggerImpl(CONSOLE_LOGGER_NAME);
      Logger customerLogger = LogManager.getLogger(CUSTOMER_LOGGER_NAMESPACE);
      Logger consoleLogger = LogManager.getLogger(CONSOLE_LOGGER_NAME);

      operatorEventLogger = new TCLoggerImpl(OPERATOR_EVENT_LOGGER_NAME);

      List<Logger> internalLoggers = new ArrayList<>();
      for (String nameSpace : INTERNAL_LOGGER_NAMESPACES) {
        internalLoggers.add(LogManager.getLogger(nameSpace));
      }

      /**
       * Don't add consoleLogger to allLoggers because it's a child of customerLogger, so it shouldn't get any appenders.
       * If you DO add consoleLogger here, you'll see duplicate messages in the log file.
       */
      allLoggers = createAllLoggerList(internalLoggers, customerLogger);

      if (!customLogging) {
        Configurator.setLevel(LogManager.getLogger("org.mortbay").getName(), Level.OFF);

        for (Logger internalLogger : internalLoggers) {
          Configurator.setLevel(internalLogger.getName(), Level.INFO);
        }
        Configurator.setLevel(customerLogger.getName(), Level.INFO);
        Configurator.setLevel(consoleLogger.getName(), Level.INFO);

        PatternMatch patternMatch[] = new PatternMatch[2];
        patternMatch[0] = PatternMatch.newBuilder().setKey(DUMP_PATTERN).setPattern(DUMP_PATTERN).build();
        patternMatch[1] = PatternMatch.newBuilder().setKey(DERBY_PATTERN).setPattern(DERBY_PATTERN).build();
        Script s = Script.createScript("selector", "JavaScript",
            "var result\n" +
            "switch (logEvent.getLoggerName())\n" +
            "                        {\n" +
            "                         case \"com.tc.dumper.dump\":result=\"[dump] %m%n\";break;\n" +
            "                         case \"com.tc.derby.log\":result=\"[derby.log] %m%n\";break;\n" +
            "                         default:result=null;\n" +
            "                         }\n" +
            "                         result;");
        LoggerContext context = LoggerContext.getContext(false);
        if (!isDev) {
          ScriptPatternSelector scriptPatternSelector = ScriptPatternSelector.newBuilder()
              .setScript(s)
              .setProperties(patternMatch)
              .setDefaultPattern(CONSOLE_PATTERN)
              .setConfiguration(context.getConfiguration())
              .build();
          PatternLayout layout = PatternLayout.newBuilder()
              .withPatternSelector(scriptPatternSelector)
              .build();
          consoleAppender = ConsoleAppender.createDefaultAppenderForLayout(layout);
          consoleAppender.start();
          // Only the console logger goes to the console (by default)
          context.getLogger(consoleLogger.getName()).addAppender(consoleAppender);
        } else {
          ScriptPatternSelector scriptPatternSelector = ScriptPatternSelector.newBuilder()
              .setScript(s)
              .setProperties(patternMatch)
              .setDefaultPattern(CONSOLE_PATTERN_DEVELOPMENT)
              .setConfiguration(context.getConfiguration())
              .build();
          PatternLayout layout = PatternLayout.newBuilder()
              .withPatternSelector(scriptPatternSelector)
              .build();
          consoleAppender = ConsoleAppender.createDefaultAppenderForLayout(layout);
          consoleAppender.start();
          // For non-customer environments, send all logging to the console...
          context.getRootLogger().addAppender(consoleAppender);
        }
      }

      delegateFileAppender = new DelegatingAppender(NullAppender.createAppender("null"));
      delegateFileAppender.start();
      addToAllLoggers(delegateFileAppender);

      BufferingAppender realBufferingAppender;
      realBufferingAppender = new BufferingAppender
          (MAX_BUFFERED_LOG_MESSAGES, "buffering appender", null, null, true);
      delegateBufferingAppender = new DelegatingAppender(realBufferingAppender);
      delegateBufferingAppender.start();
      addToAllLoggers(delegateBufferingAppender);
      buffering = true;

      if (!isDev) {
        CustomerLogging.getGenericCustomerLogger().info("New logging session started.");
      }

      writeVersion();
      writePID();
      writeLoggingConfigurations();
    } catch (Exception e) {
      reportLoggingError(e);
    } finally {
      Thread.currentThread().setContextClassLoader(prevLoader);
    }
  }

  // for test use only!
  public static Log4JAppenderToTCAppender addAppender(String loggerName, TCAppender appender) {
    Log4JAppenderToTCAppender wrappedAppender =
        new Log4JAppenderToTCAppender(appender, "Log4J2TCAppender", null, null, true);
    wrappedAppender.start();
    LoggerContext context = LoggerContext.getContext(false);
    context.getLogger(new TCLoggerImpl(loggerName).getLogger().getName()).addAppender(wrappedAppender);
    return wrappedAppender;
  }

  public static void removeAppender(String loggerName, Log4JAppenderToTCAppender appender) {
    LoggerContext context = LoggerContext.getContext(false);
    context.getLogger(new TCLoggerImpl(loggerName).getLogger().getName()).removeAppender(appender);
  }

  private static Logger[] createAllLoggerList(List<Logger> internalLoggers, Logger customerLogger) {
    List<Logger> loggers = new ArrayList<Logger>();
    loggers.addAll(internalLoggers);
    loggers.add(customerLogger);
    return loggers.toArray(new Logger[] {});
  }

  public static void addToAllLoggers(Appender appender) {
    LoggerContext context = LoggerContext.getContext(false);
    for (Logger allLogger : allLoggers) {
      context.getLogger(allLogger.getName()).addAppender(appender);
    }
  }

  private static void writeVersion() {
    ProductInfo info = ProductInfo.getInstance();
    TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

    // Write build info always
    String longProductString = info.toLongString();
    consoleLogger.info(longProductString);

    // Write patch info, if any
    if (info.isPatched()) {
      String longPatchString = info.toLongPatchString();
      consoleLogger.info(longPatchString);
    }

    String versionMessage = info.versionMessage();
    if (!versionMessage.isEmpty()) {
      consoleLogger.info(versionMessage);
    }
  }

  private static void writePID() {
    try {
      String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      long pid = Long.parseLong(processName.split("@")[0]);
      getLogger(TCLogging.class).info("PID is " + pid);
    } catch (Throwable t) {
      // ignore, not fatal if this doesn't work for some reason
    }
  }

  private static void writeSystemProperties() {
    try {
      Properties properties = System.getProperties();
      int maxKeyLength = 1;

      ArrayList keys = new ArrayList();
      Iterator iter = properties.entrySet().iterator();
      while (iter.hasNext()) {
        Entry entry = (Entry) iter.next();
        Object objKey = entry.getKey();
        Object objValue = entry.getValue();

        // Filter out any bad non-String keys or values in system properties
        if (objKey instanceof String && objValue instanceof String) {
          String key = (String) objKey;
          keys.add(key);
          maxKeyLength = Math.max(maxKeyLength, key.length());
        }
      }

      String inputArguments = null;
      try {
        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        inputArguments = mxbean.getInputArguments().toString();
      } catch (SecurityException se) {
        inputArguments = "unknown";
      }
      String nl = System.getProperty("line.separator");
      StringBuffer data = new StringBuffer();
      data.append("All Java System Properties for this Terracotta instance:");
      data.append(nl);
      data.append("========================================================================");
      data.append(nl);
      data.append("JVM arguments: " + inputArguments);
      data.append(nl);

      String[] sortedKeys = (String[]) keys.toArray(new String[keys.size()]);
      Arrays.sort(sortedKeys);
      for (String key : sortedKeys) {
        data.append(StringUtils.rightPad(key, maxKeyLength));
        data.append(": ");
        data.append(properties.get(key));
        data.append(nl);
      }
      data.append("========================================================================");

      getLogger(TCLogging.class).info(data.toString());
    } catch (Throwable t) {
      // don't let exceptions here be fatal
      t.printStackTrace();
    }
  }

  // This method for use in tests only
  public static void closeFileAppender() {
    if (delegateFileAppender != null) delegateFileAppender.stop();
  }

  /**
   * This method will print the logging configurations being used by the logger.
   */
  private static void writeLoggingConfigurations() {
    if (loggingProperties != null) {
      getLogger(TCLogging.class).info(LOG_CONFIGURATION_PREFIX + loggingProperties);
    }
  }

}
