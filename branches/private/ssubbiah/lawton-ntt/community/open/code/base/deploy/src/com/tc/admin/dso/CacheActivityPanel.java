/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin.dso;

import org.jfree.chart.JFreeChart;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.DemoChartFactory;
import com.tc.admin.common.MultiStatisticPanel;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.Poller;

import java.awt.BorderLayout;

import javax.management.ObjectName;

public class CacheActivityPanel extends XContainer implements Poller {
  private MultiStatisticPanel m_panel;

  public CacheActivityPanel(ConnectionContext cc, ObjectName bean) {
    super(new BorderLayout());

    AdminClientContext acc = AdminClient.getContext();

    String[] stats = {
      "ObjectFlushRate",
      "ObjectFaultRate",
    };

    String[] names = {
      acc.getMessage("dso.object.flush.rate"),
      acc.getMessage("dso.object.fault.rate")
    };

    String header = acc.getMessage("dso.cache.activity");
    String xAxis  = null;
    String yAxis  = acc.getMessage("dso.cache.rate.range.label");

    m_panel = new MultiStatisticPanel(cc, bean, stats, names, header, xAxis, yAxis) {
      public JFreeChart createChart() {
        return DemoChartFactory.getXYLineChart("", "", "", m_timeSeries);
      }
    };
    add(m_panel);
  }

  public boolean isRunning() {
    return m_panel != null && m_panel.isRunning();
  }
  
  public void stop() {
    if(isRunning())
      m_panel.stop();
  }

  public void start() {
    if(!isRunning())
      m_panel.start();
  }
  
  public void tearDown() {
    if(isRunning())
      m_panel.stop();

    super.tearDown();
    m_panel = null;
  }
}
