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
package com.tc.util.runtime;

/**
 * Utility class for understanding the current JVM version. Access the VM
 * version information by looking at {@link #VERSION} directly or calling the
 * static helper methods.
 */
public class Vm {

  /**
   * Version info is parsed from system properties and stored here.
   */
  public static final VmVersion VERSION;
  static {
    VERSION = new VmVersion(System.getProperties());
  }

  private Vm() {
    // utility class
  }

  /**
   * True if IBM JDK
   * 
   * @return True if IBM JDK
   */
  public static boolean isIBM() {
    return VERSION.isIBM();
  }

  /**
   * True if JRockit
   * 
   * @return True if BEA Jrockit VM
   */
  public static boolean isJRockit() {
    return VERSION.isJRockit();
  }

  /**
   * Returns the value matching what is found in our startup script as we can no longer find that value with an API
   * starting in Java 9.
   *
   * @return the value matching our startup script
   */
  public static long maxDirectMemory() {
    // Returning a constant as the code is no longer present in Java 9 and this is what we pass in startup script
    return 9223372036854775807L;
  }
}
