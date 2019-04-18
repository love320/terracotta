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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.tc.exception.TCRuntimeException;
import com.tc.management.beans.logging.TCLoggingBroadcaster;
import com.tc.management.beans.logging.TCLoggingBroadcasterMBean;

import java.io.Serializable;

import javax.management.NotCompliantMBeanException;

/**
 * Special Appender that notifies JMX listeners on LoggingEvents.
 *
 * @author gkeim
 * @see org.apache.logging.log4j.core.appender.RollingFileAppender
 * @see TCLoggingBroadcasterMBean
 */

public class JMXAppender extends AbstractAppender {

  private final TCLoggingBroadcaster broadcastingBean;

  public JMXAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
    super(name, filter, layout, ignoreExceptions);
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
  public void append(final LogEvent event) {
    //broadcastingBean.broadcastLogEvent(getLayout().format(event), event.getThrowableStrRep());
    broadcastingBean.broadcastLogEvent
        (((PatternLayout)getLayout()).toSerializable(event),
            event.getThrown() != null ? event.getThrown().getMessage() : null);
  }

}
