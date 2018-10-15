package com.tc.net.core;

import com.tc.net.core.security.TCSecurityManager;

/**
 * Created by alsu on 27/01/16.
 */
public class BufferManagerFactoryProviderImpl implements BufferManagerFactoryProvider {
  
  private final TCSecurityManager securityManager;

  public BufferManagerFactoryProviderImpl(final TCSecurityManager securityManager) {
    this.securityManager = securityManager;
  }


  @Override
  public BufferManagerFactory getBufferManagerFactory() {
    if (securityManager != null) {
      return securityManager.getBufferManagerFactory();
    } else {
      return new ClearTextBufferManagerFactory();
    }
  }
}
