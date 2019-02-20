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
package com.tc.admin.wrapper;

/**
 * This class is used by the Tanuki Service Wrapper. The real TCStop interferes with the wrapper's
 * shutdown facilities. The wrapper "stop" class is meant to simply clean up prior to the JVM being terminated
 * by the wrapper. When the stop class itself invokes System.exit problems ensue.
 *
 * The Terracotta Tanuki integration implements Integration Method #2 (WrapperStartStopApp) and that information
 * is encoded in the Tanuki license. It would be preferable to instead implement the simpler Integration Method #1
 * (WrapperSimpleApp) that only requires a "start" class, obviating the need for this class.
 *
 * TODO: if/when the license is ever updated we should take that opportunity to move to Integration Method #1
 */

public class TCStop {
  public static void main(String[] args) {
    return;
  }
}
