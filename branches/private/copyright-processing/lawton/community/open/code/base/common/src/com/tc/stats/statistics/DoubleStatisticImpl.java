/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.statistics;

public class DoubleStatisticImpl extends StatisticImpl implements DoubleStatistic {

  private double doubleValue;

  public DoubleStatisticImpl(String name, String unit, String desc, long startTime, long lastSampleTime) {
    super(name, unit, desc, startTime, lastSampleTime);
  }

  public double getDoubleValue() {
    return this.doubleValue;
  }

  public void setDoubleValue(double doubleValue) {
    this.doubleValue = doubleValue;
  }
}
