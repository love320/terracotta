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
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.tc.util.Assert;

public class DelegatingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  private Appender delegate;

  public DelegatingAppender(Appender delegate) {
    Assert.assertNotNull(delegate);
    this.delegate = delegate;
  }

  private synchronized Appender delegate() {
    return this.delegate;
  }

  public synchronized Appender setDelegate(Appender delegate) {
    Assert.assertNotNull(delegate);
    Appender prev = this.delegate;
    this.delegate = delegate;
    return prev;
  }

  @Override
  public void append(ILoggingEvent event) throws LogbackException {
    delegate().doAppend(event);
  }

  public void closeDelegate() {
    delegate().stop();
  }

  public void close() {
    closeDelegate();
  }
}
