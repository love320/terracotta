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

import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

import com.tc.util.Assert;

import java.io.Serializable;

public class DelegatingAppender extends AbstractLifeCycle implements Appender {

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
    Appender out = this.delegate;
    this.delegate = delegate;
    return out;
  }

  @Override
  public void append(LogEvent logEvent) {
    delegate().append(logEvent);
  }

  @Override
  public String getName() {
    return delegate().getName();
  }

  @Override
  public Layout<? extends Serializable> getLayout() {
    return delegate().getLayout();
  }

  @Override
  public boolean ignoreExceptions() {
    return true;
  }

  @Override
  public ErrorHandler getHandler() {
    return delegate().getHandler();
  }

  @Override
  public void setHandler(ErrorHandler errorHandler) {
    delegate().setHandler(errorHandler);
  }
}
