/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.intercept;

import com.tc.aspectwerkz.joinpoint.JoinPoint;

/**
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bon�r </a>
 */
public interface AroundAdvice extends Advice {

  /**
   * @param jp
   * @throws Throwable
   */
  Object invoke(JoinPoint jp) throws Throwable;
}
