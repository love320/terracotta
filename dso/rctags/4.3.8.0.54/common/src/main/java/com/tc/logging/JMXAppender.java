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
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import ch.qos.logback.core.util.CachingDateFormatter;
import com.tc.exception.TCRuntimeException;
import com.tc.management.beans.logging.TCLoggingBroadcaster;
import com.tc.management.beans.logging.TCLoggingBroadcasterMBean;

import javax.management.NotCompliantMBeanException;

/**
 * Special Appender that notifies JMX listeners on LoggingEvents.
 *
 * @author gkeim
 * @see TCLoggingBroadcasterMBean
 */
public class JMXAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private final TCLoggingBroadcaster broadcastingBean;
  private final CachingDateFormatter cachingDateFormatter = new CachingDateFormatter("HH:mm:ss.SSS");

  public JMXAppender() {
    try {
      broadcastingBean = new TCLoggingBroadcaster();
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the broadcasting MBean: this is a programming error in "
                                   + TCLoggingBroadcaster.class.getName(), ncmbe);
    }
  }

  public final TCLoggingBroadcasterMBean getMBean() {
    return broadcastingBean;
  }

  @Override
  public void append(ILoggingEvent iLoggingEvent) {
    broadcastingBean.broadcastLogEvent(format(iLoggingEvent));
  }

  private String format(ILoggingEvent event) {
    StringBuilder sb = new StringBuilder();
    long timestamp = event.getTimeStamp();

    sb.append(cachingDateFormatter.format(timestamp));
    sb.append(" [");
    sb.append(event.getThreadName());
    sb.append("] ");
    sb.append(event.getLevel().toString());
    sb.append(" ");
    sb.append(event.getLoggerName());
    sb.append(" - ");
    sb.append(event.getFormattedMessage());
    IThrowableProxy tp = event.getThrowableProxy();
    if (tp != null) {
      sb.append(CoreConstants.LINE_SEPARATOR);
      sb.append(ThrowableProxyUtil.asString(tp).trim());
    }

    return sb.toString();
  }
}
