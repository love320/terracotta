/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

/**
 * Wrap exception in a nice block message surrounded by ****s
 */
public class ExceptionWrapperImpl implements ExceptionWrapper {

  private final int MAX_STAR_COUNT = 79;

  public String wrap(String message) {
    message = (message == null) ? String.valueOf(message) : message;
    int starCount = longestLineCharCount(message);
    if(starCount > MAX_STAR_COUNT) {
      starCount = MAX_STAR_COUNT;
    }
    return "\n" + getStars(starCount) + "\n" + message
           + "\n" + getStars(starCount) + "\n";
  }

  private String getStars(int starCount) {
    StringBuffer stars = new StringBuffer();
    while(starCount-- > 0) {
      stars.append('*');
    }
    return stars.toString();
  }

  private int longestLineCharCount(String message) {
    int count = 0;
    int sidx = 0, eidx = 0;
    while ((eidx = message.indexOf('\n', sidx)) >= 0) {
      if (count < (eidx - sidx)) {
        count = (eidx - sidx);
      }
      sidx = eidx + 1;
    }
    if (sidx < message.length() && count < (message.length() - sidx)) {
      count = (message.length() - sidx);
    }
    return count;
  }

}
