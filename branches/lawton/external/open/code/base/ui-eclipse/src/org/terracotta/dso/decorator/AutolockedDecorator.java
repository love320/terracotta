package org.terracotta.dso.decorator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import org.terracotta.dso.ConfigurationHelper;
import org.terracotta.dso.TcPlugin;

/**
 * Adorns Java methods that are autolocked.
 * 
 * The adornment appears in the Package Explorer amd Outline view.
 * 
 * @see org.eclipse.jface.viewers.LabelProvider
 * @see org.terracotta.dso.ConfigurationHelper.isAutolocked
 */

public class AutolockedDecorator extends LabelProvider
  implements ILightweightLabelDecorator
{
  private static final ImageDescriptor
    m_imageDesc = ImageDescriptor.createFromURL(
      AutolockedDecorator.class.getResource(
        "/com/tc/admin/icons/autolocked_ovr.gif"));

  public static final String
    DECORATOR_ID = "org.terracotta.dso.autoLockedDecorator";

  public void decorate(Object element, IDecoration decoration) {
    TcPlugin plugin  = TcPlugin.getDefault();
    IMethod  method  = (IMethod)element;
    IProject project = method.getJavaProject().getProject();    

    if(plugin.hasTerracottaNature(project)) {
      ConfigurationHelper config = plugin.getConfigurationHelper(project);

      if(config != null && config.isAutolocked(method)){
        decoration.addOverlay(m_imageDesc);
      }
    }
  }
  
  public static void updateDecorators() {
    TcPlugin.getDefault().updateDecorator(DECORATOR_ID);
  }
}
