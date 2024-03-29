/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.AggregateServerRuntimeStatsPanel;
import com.tc.admin.ClusterElementChooser;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.ServerGroupsNode;
import com.tc.admin.ServerNode;
import com.tc.admin.ServerRuntimeStatsPanel;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class RuntimeStatsPanel extends XContainer implements ActionListener, ClientConnectionListener,
    PropertyChangeListener {
  private IAdminClientContext adminClientContext;
  private IClusterModel       clusterModel;
  private ClusterListener     clusterListener;
  private ElementChooser      elementChooser;
  private PagedView           pagedView;
  private boolean             inited;

  private static final String AGGREGATE_SERVER_STATS_NODE_NAME = "AggregateServerStatsNode";

  private static final String EMPTY_PAGE                       = "EmptyPage";

  public RuntimeStatsPanel(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    add(pagedView = new PagedView(), BorderLayout.CENTER);

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.EAST;

    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(new XLabel(), gbc);
    gbc.gridx++;

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;

    topPanel.add(new XLabel("View:"), gbc);
    gbc.gridx++;

    topPanel.add(elementChooser = new ElementChooser(), gbc);
    elementChooser.addActionListener(this);

    topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
    add(topPanel, BorderLayout.NORTH);

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      addNodePanels();
    }
  }

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(clusterModel, RuntimeStatsPanel.this);
    }

    protected XTreeNode[] createTopLevelNodes() {
      ComponentNode aggregateServerStatsNode = new ComponentNode("Aggregate Server Stats");
      ClientsNode clientsNode = new ClientsNode(adminClientContext, clusterModel);
      ServerGroupsNode serverGroupsNode = new ServerGroupsNode(adminClientContext, clusterModel);

      aggregateServerStatsNode.setName(AGGREGATE_SERVER_STATS_NODE_NAME);
      clientsNode.setLabel("Per Client View");
      serverGroupsNode.setLabel("Per Server View");

      return new XTreeNode[] { aggregateServerStatsNode, clientsNode, serverGroupsNode };
    }

    protected boolean acceptPath(TreePath path) {
      Object o = path.getLastPathComponent();
      if (o instanceof XTreeNode) {
        XTreeNode node = (XTreeNode) o;
        return AGGREGATE_SERVER_STATS_NODE_NAME.equals(node.getName()) || node instanceof ClientNode
               || node instanceof ServerNode;
      }
      return false;
    }
  }

  public void actionPerformed(ActionEvent e) {
    ElementChooser chsr = (ElementChooser) e.getSource();
    XTreeNode node = (XTreeNode) chsr.getSelectedObject();
    String name = node.getName();
    if (pagedView.hasPage(name)) {
      pagedView.setPage(name);
    } else {
      pagedView.setPage(EMPTY_PAGE);
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    protected void handleReady() {
      if (!inited && clusterModel.isReady()) {
        addNodePanels();
      }
    }

    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(RuntimeStatsPanel.this);
      }
      if (newActive != null) {
        newActive.removeClientConnectionListener(RuntimeStatsPanel.this);
      }
    }
  }

  public void clientConnected(final IClient client) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        pagedView.addPage(createClientViewPanel(client));
      }
    });
  }

  public void clientDisconnected(final IClient client) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        pagedView.remove(pagedView.getPage(client.toString()));
      }
    });
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (PagedView.PROP_CURRENT_PAGE.equals(prop)) {
      String newPage = (String) evt.getNewValue();
      elementChooser.setSelectedPath(newPage);
    }
  }

  private void addNodePanels() {
    pagedView.removeAll();
    XLabel emptyPage = new XLabel();
    emptyPage.setName(EMPTY_PAGE);
    pagedView.addPage(emptyPage);

    pagedView.addPage(createAggregateServerStatsPanel());
    for (IServerGroup group : clusterModel.getServerGroups()) {
      for (IServer server : group.getMembers()) {
        pagedView.addPage(createServerViewPanel(server));

        if (server.isActiveCoordinator()) {
          for (IClient client : server.getClients()) {
            pagedView.addPage(createClientViewPanel(client));
          }
          server.addClientConnectionListener(this);
        }
      }
    }
    elementChooser.setSelectedPath(AGGREGATE_SERVER_STATS_NODE_NAME);
    pagedView.addPropertyChangeListener(this);
    inited = true;
  }

  private AggregateServerRuntimeStatsPanel createAggregateServerStatsPanel() {
    AggregateServerRuntimeStatsPanel panel = new AggregateServerRuntimeStatsPanel(adminClientContext, clusterModel);
    panel.setName(AGGREGATE_SERVER_STATS_NODE_NAME);
    return panel;
  }

  private ServerRuntimeStatsPanel createServerViewPanel(IServer server) {
    ServerRuntimeStatsPanel panel = new ServerRuntimeStatsPanel(adminClientContext, server);
    panel.setName(server.toString());
    return panel;
  }

  private ClientRuntimeStatsPanel createClientViewPanel(IClient client) {
    ClientRuntimeStatsPanel panel = new ClientRuntimeStatsPanel(adminClientContext, client);
    panel.setName(client.toString());
    return panel;
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    pagedView.removePropertyChangeListener(this);
    elementChooser.removeActionListener(this);

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      clusterListener = null;
      elementChooser.tearDown();
      elementChooser = null;
      pagedView = null;
    }
  }
}
