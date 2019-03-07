package com.terracotta.toolkit.collections;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ListAppender
 */

public class ListAppender extends AbstractAppender {
  List<LogEvent> list;

  public ListAppender(List<LogEvent> list, String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
    super(name, filter, layout, ignoreExceptions);
    this.list = list;
  }

  @Override
  public void append(LogEvent logEvent) {
    list.add(logEvent);
  }

}
