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
  public static final String POLARION_FETCH_PROJECT_ID = "sonar.polarion.fetch.project.id";
  public static final String POLARION_CREATE_PROJECT_ID = "sonar.polarion.create.project.id";
  public static final String POLARION_USERNAME_PROPERTY = "sonar.polarion.login.secured";
  public static final String POLARION_PASSWORD_PROPERTY = "sonar.polarion.password.secured";

}
