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
package org.sonar.plugins.polarion.reviews;

import org.sonar.api.Properties;
import org.sonar.api.Property;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.action.Function;
import org.sonar.plugins.polarion.PolarionConstants;


@Properties({
  @Property(
    key = PolarionConstants.POLARION_CREATE_PROJECT_ID,
    name = "Project Create Id",
    description = "Project ID of Polarion project where defect shall be created when linking SonarQube issue to Polarion. Case sensitive, example : elibrary",
    global = false,
    project = true,
    module = true
  )
})

public class LinkFunction implements Function, ServerExtension {

  private final PolarionIssueCreator polarionIssueCreator;

  public LinkFunction(PolarionIssueCreator polarionIssueCreator) {
    this.polarionIssueCreator = polarionIssueCreator;
  }

  public void execute(Context context) {
    checkConditions(context.projectSettings());
    createPolarionIssue(context);
  }

  protected void createPolarionIssue(Context context){
    String issue;
    try {
      issue = polarionIssueCreator.createIssue(context.issue(), context.projectSettings());
    } catch (Exception e) {
      throw new IllegalStateException("Impossible to create an issue on Polarion. A problem occured with the remote server: " + e.getMessage(), e);
    }

    createComment(issue, context);
    // and add the property
    context.setAttribute(PolarionConstants.SONAR_ISSUE_DATA_PROPERTY_KEY, issue);
  }

  @VisibleForTesting
  void checkConditions(Settings settings) {
    checkProperty(PolarionConstants.SERVER_URL_PROPERTY, settings);
    checkProperty(PolarionConstants.POLARION_USERNAME_PROPERTY, settings);
    checkProperty(PolarionConstants.POLARION_PASSWORD_PROPERTY, settings);
    checkProperty(PolarionConstants.POLARION_FETCH_PROJECT_ID, settings);
    checkProperty(PolarionConstants.POLARION_CREATE_PROJECT_ID, settings);
    }

  private void checkProperty(String property, Settings settings) {
    if (!settings.hasKey(property) && !settings.hasDefaultValue(property)) {
      throw new IllegalStateException("The Polarion property \""+ property + "\" must be defined before you can use the \"Link to Polarion\" button");
    }
  }

  protected void createComment(String issue, Context context) {
    context.addComment(generateCommentText(issue, context));
  }

  protected String generateCommentText(String issue, Context context) {
    StringBuilder message = new StringBuilder();
    message.append("Issue linked to Polarion issue: ");
    message.append(context.projectSettings().getString(PolarionConstants.SERVER_URL_PROPERTY));
    message.append("/polarion/#/project/");
    message.append(context.projectSettings().getString(PolarionConstants.POLARION_CREATE_PROJECT_ID));
    message.append("/workitem?id=");
    message.append(issue);
    return message.toString();
  }
}
