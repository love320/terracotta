/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.terracotta.session;

import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.management.beans.sessions.SessionMonitorMBean.SessionsComptroller;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.terracotta.session.util.Assert;
import com.terracotta.session.util.ContextMgr;
import com.terracotta.session.util.DefaultContextMgr;
import com.terracotta.session.util.LifecycleEventMgr;
import com.terracotta.session.util.Lock;
import com.terracotta.session.util.SessionCookieWriter;
import com.terracotta.session.util.SessionIdGenerator;
import com.terracotta.session.util.StandardSession;
import com.terracotta.session.util.Timestamp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TerracottaSessionManager {

  private final SessionMonitorMBean    mBean;
  private final SessionIdGenerator     idGenerator;
  private final SessionCookieWriter    cookieWriter;
  private final SessionDataStore       store;
  private final LifecycleEventMgr      eventMgr;
  private final ContextMgr             contextMgr;
  private final boolean                logEnabled;
  private final TCLogger               logger;
  private final RequestResponseFactory factory;

  public TerracottaSessionManager(SessionIdGenerator sig, SessionCookieWriter scw, LifecycleEventMgr eventMgr,
                                  ContextMgr contextMgr, int maxIdleSeconds, int invalidatorSleepSeconds,
                                  boolean logEnabled, RequestResponseFactory factory) {
    Assert.pre(sig != null);
    Assert.pre(scw != null);
    Assert.pre(eventMgr != null);
    Assert.pre(contextMgr != null);

    this.idGenerator = sig;
    this.cookieWriter = scw;
    this.eventMgr = eventMgr;
    this.contextMgr = contextMgr;
    this.factory = factory;
    this.store = new SessionDataStore(contextMgr.getAppName(), maxIdleSeconds);
    this.logger = ManagerUtil.getLogger("com.tc.tcsession." + contextMgr.getAppName());
    this.logEnabled = logEnabled;

    Thread invalidator = new Thread(new SessionInvalidator(store, invalidatorSleepSeconds, maxIdleSeconds),
                                    "SessionInvalidator - " + contextMgr.getAppName());
    invalidator.setDaemon(true);
    invalidator.start();
    Assert.post(invalidator.isAlive());

    // This is disgusting, but right now we have to do this because we don't have an event
    // management infrastructure to boot stuff up
    mBean = ManagerUtil.getSessionMonitorMBean();

    mBean.registerSessionsController(new SessionsComptroller() {
      public boolean killSession(final String browserSessionId) {
        SessionId id = idGenerator.makeInstanceFromBrowserId(browserSessionId);
        if (id == null) {
          // that, potentially, was not *browser* id, try to recover...
          id = idGenerator.makeInstanceFromInternalKey(browserSessionId);
        }
        expire(id);
        return true;
      }

    });
  }

  public TerracottaRequest preprocess(HttpServletRequest req, HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);

    SessionId sessionId = findSessionId(req);
    TerracottaRequest rw = wrapRequest(sessionId, req, res);

    Assert.post(rw != null);
    return rw;
  }

  public TerracottaResponse createResponse(TerracottaRequest req, HttpServletResponse res) {
    return factory.createResponse(req, res);
  }

  public void postprocess(TerracottaRequest req) {
    Assert.pre(req != null);

    mBean.requestProcessed();
    final Session session = req.getSessionIfAny();
    if (session == null) return;
    final SessionId id = session.getSessionId();
    final SessionData sd = session.getSessionData();
    try {
      if (!session.isValid()) store.remove(id);
      else {
        sd.finishRequest();
        store.updateTimestampIfNeeded(sd);
      }
    } finally {
      if (req.isUnlockSesssionId()) id.commitLock();
      if (logEnabled) {
        final String msg = "REQUEST BENCH: url=[" + req.getRequestURL() + "] sid=[" + id.getKey() + "] -> ["
                           + id.getLock().getLockTimer().elapsed() + ":" + id.getLock().getUnlockTimer().elapsed()
                           + "] -> " + (System.currentTimeMillis() - req.getRequestStartMillis());
        logger.info(msg);
      }
    }
  }

  /**
   * The only use for this method [currently] is by Struts' Include Tag, which can generate a nested request. In this
   * case we have to release session lock, so that nested request (running, potentially, in another JVM) can acquire it.
   * {@link TerracottaSessionManager#resumeRequest(Session)} method will re-aquire the lock.
   */
  public static void pauseRequest(final Session sess) {
    Assert.pre(sess != null);
    final SessionId id = sess.getSessionId();
    final SessionData sd = sess.getSessionData();
    sd.finishRequest();
    id.commitLock();
  }

  /**
   * See {@link TerracottaSessionManager#resumeRequest(Session)} for details
   */
  public static void resumeRequest(final Session sess) {
    Assert.pre(sess != null);
    final SessionId id = sess.getSessionId();
    final SessionData sd = sess.getSessionData();
    id.getWriteLock();
    sd.startRequest();
  }

  private TerracottaRequest wrapRequest(SessionId sessionId, HttpServletRequest req, HttpServletResponse res) {
    TerracottaRequest request = factory.createRequest(sessionId, req, res);
    request.setSessionManager(this);
    return request;
  }

  /**
   * This method always returns a valid session. If data for the requestedSessionId found and is valid, it is returned.
   * Otherwise, we must create a new session id, a new session data, a new sessiono, and cookie the response.
   */
  protected Session getSession(final SessionId requestedSessionId, final HttpServletRequest req,
                               final HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);
    Session rv = doGetSession(requestedSessionId, req, res);
    Assert.post(rv != null);
    return rv;
  }

  protected Session getSessionIfExists(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res) {
    if (requestedSessionId == null) return null;
    SessionData sd = store.find(requestedSessionId);
    if (sd == null) return null;
    Assert.inv(sd.isValid());
    if (requestedSessionId.isServerHop()) cookieWriter.writeCookie(req, res, requestedSessionId);
    return new StandardSession(requestedSessionId, sd, eventMgr, contextMgr);
  }

  protected SessionCookieWriter getCookieWriter() {
    return this.cookieWriter;
  }

  private void expire(SessionId id) {
    SessionData sd = null;
    try {
      sd = store.find(id);
      if (sd != null) {
        expire(id, sd);
      }
    } finally {
      if (sd != null) id.commitLock();
    }
  }

  private void expire(SessionId id, SessionData sd) {
    StandardSession sess = new StandardSession(id, sd, eventMgr, contextMgr);
    sess.invalidate();
    store.remove(id);
    mBean.sessionDestroyed();
  }

  private Session doGetSession(final SessionId requestedSessionId, final HttpServletRequest req,
                               final HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);

    if (requestedSessionId == null) { return createNewSession(req, res); }
    final SessionData sd = store.find(requestedSessionId);
    if (sd == null) { return createNewSession(req, res); }
    Assert.inv(sd.isValid());
    if (requestedSessionId.isServerHop()) cookieWriter.writeCookie(req, res, requestedSessionId);
    return new StandardSession(requestedSessionId, sd, eventMgr, contextMgr);
  }

  private Session createNewSession(HttpServletRequest req, HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);

    SessionId id = idGenerator.generateNewId();
    SessionData sd = store.createSessionData(id);
    Session rv = new StandardSession(id, sd, eventMgr, contextMgr);
    cookieWriter.writeCookie(req, res, id);
    eventMgr.fireSessionCreatedEvent(rv);
    mBean.sessionCreated();
    Assert.post(rv != null);
    return rv;
  }

  private SessionId findSessionId(HttpServletRequest httpRequest) {
    Assert.pre(httpRequest != null);

    String requestedSessionId = httpRequest.getRequestedSessionId();
    if (requestedSessionId == null) return null;
    else return idGenerator.makeInstanceFromBrowserId(requestedSessionId);
  }

  class SessionInvalidator implements Runnable {

    private final long sleepMillis;

    public SessionInvalidator(final SessionDataStore store, final long sleepSeconds,
                              final long defaultSessionIdleSeconds) {
      this.sleepMillis = sleepSeconds * 1000;
    }

    public void run() {
      final String invalidatorLock = "tc:session_invalidator_lock_" + contextMgr.getAppName();

      while (true) {
        sleep(sleepMillis);
        if (Thread.interrupted()) {
          break;
        } else {
          final Lock lock = new Lock(invalidatorLock);
          lock.tryWriteLock();
          if (!lock.isLocked()) continue;
          try {
            invalidateSessions();
          } finally {
            lock.commitLock();
          }
        }
      }
    }

    private void invalidateSessions() {
      final long startMillis = System.currentTimeMillis();
      final String keys[] = store.getAllKeys();
      int totalCnt = 0;
      int invalCnt = 0;
      int evaled = 0;
      int notEvaled = 0;
      for (int i = 0; i < keys.length; i++) {
        final String key = keys[i];
        final SessionId id = idGenerator.makeInstanceFromInternalKey(key);
        final Timestamp dtm = store.findTimestampUnlocked(id);
        if (dtm == null) continue;
        totalCnt++;
        if (dtm.getMillis() < System.currentTimeMillis()) {
          evaled++;
          if (evaluateSession(dtm, id)) invalCnt++;
        } else {
          notEvaled++;
        }
      }
      if (logEnabled) {
        final String msg = "SESSION INVALIDATOR BENCH: " + " -> total=" + totalCnt + ", evaled=" + evaled
                           + ", notEvaled=" + notEvaled + ", invalidated=" + invalCnt + " -> elapsed="
                           + (System.currentTimeMillis() - startMillis);
        logger.info(msg);
      }
    }

    private boolean evaluateSession(final Timestamp dtm, final SessionId id) {
      Assert.pre(id != null);

      boolean rv = false;
      id.tryWriteLock();
      if (!id.getLock().isLocked()) { return rv; }

      try {
        final SessionData sd = store.findSessionDataUnlocked(id);
        if (sd == null) return rv;
        if (!sd.isValid()) {
          expire(id, sd);
          rv = true;
        } else {
          store.updateTimestampIfNeeded(sd);
        }
      } finally {
        id.commitLock();
      }
      return rv;
    }

    private void sleep(long l) {
      try {
        Thread.sleep(l);
      } catch (InterruptedException ignore) {
        // nothing to do
      }
    }
  }

  public static boolean isDsoSessionApp(HttpServletRequest request) {
    Assert.pre(request != null);
    final String appName = DefaultContextMgr.computeAppName(request);
    return ClassProcessorHelper.isDSOSessions(appName);
  }

}
