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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class LogBackAppenderToTCAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private final TCAppender appender;

  public LogBackAppenderToTCAppender(TCAppender appender) {
    this.appender = appender;
  }

  @Override
  public void append(ILoggingEvent iLoggingEvent) {
    Throwable throwable = null;
    if (iLoggingEvent.getThrowableProxy() != null && iLoggingEvent.getThrowableProxy() instanceof ThrowableProxy) {
      throwable = ((ThrowableProxy) iLoggingEvent.getThrowableProxy()).getThrowable();
    }
    appender.append(LogLevelImpl.fromLogBackLevel(iLoggingEvent.getLevel()), iLoggingEvent.getMessage(), throwable);
  }
}
