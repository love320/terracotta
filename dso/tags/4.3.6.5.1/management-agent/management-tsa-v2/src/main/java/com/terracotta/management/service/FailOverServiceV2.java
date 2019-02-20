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
package com.terracotta.management.service;

/**
 * An interface for service implementations providing facilities for performing fail-over actions on a TSA node.
 */
public interface FailOverServiceV2 {

  /**
   * Perform the PROMOTE fail-over action that promotes a server that is waiting for promotion
   */
  void promote();

  /**
   * Perform the RESTART fail-over action that restarts a server that is waiting for promotion
   */
  void restart();

  /**
   * Perform the FAILFAST fail-over action that shuts down a server that is waiting for promotion
   */
  void failFast();
}
