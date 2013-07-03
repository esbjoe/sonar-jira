/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.polarion;

public final class PolarionConstants {

  private PolarionConstants() {
  }

  public static final String SONAR_ISSUE_DATA_PROPERTY_KEY = "polarion-issue-key";

  // ===================== PLUGIN PROPERTIES =====================

  public static final String SERVER_URL_PROPERTY = "sonar.polarion.url";
  public static final String POLARION_PROJECT_ID = "sonar.polarion.project.id";
  //public static final String POLARION_USERNAME_PROPERTY = "sonar.polarion.login.secured"; //TODO
  public static final String POLARION_USERNAME_PROPERTY = "sonar.polarion.login";
  //public static final String POLARION_PASSWORD_PROPERTY = "sonar.polarion.password.secured"; //TODO
  public static final String POLARION_PASSWORD_PROPERTY = "sonar.polarion.password";


  //public static final String SOAP_BASE_URL_PROPERTY = "sonar.polarion.soap.url";
  //public static final String SOAP_BASE_URL_DEF_VALUE = "/rpc/soap/jirasoapservice-v2";


  /*public static final String POLARION_PROJECT_KEY_PROPERTY = "sonar.polarion.project.key";

  public static final String POLARION_INFO_PRIORITY_ID = "sonar.polarion.info.priority.id";
  public static final String POLARION_MINOR_PRIORITY_ID = "sonar.polarion.minor.priority.id";
  public static final String POLARION_MAJOR_PRIORITY_ID = "sonar.polarion.major.priority.id";
  public static final String POLARION_CRITICAL_PRIORITY_ID = "sonar.polarion.critical.priority.id";
  public static final String POLARION_BLOCKER_PRIORITY_ID = "sonar.polarion.blocker.priority.id";

  public static final String POLARION_ISSUE_TYPE_ID = "sonar.polarion.issue.type.id";

  public static final String POLARION_ISSUE_COMPONENT_ID = "sonar.polarion.issue.component.id";
  public static final String POLARION_ISSUE_COMPONENT_ID_BLANK = "<none>";
  */
}
