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

import com.polarion.alm.ws.client.types.tracker.WorkItem;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.action.Function;
import org.sonar.plugins.polarion.PolarionConstants;

import java.rmi.RemoteException;

public class LinkFunction implements Function, ServerExtension {

  private final PolarionIssueCreator PolarionIssueCreator;

  public LinkFunction(PolarionIssueCreator jiraIssueCreator) {
    this.PolarionIssueCreator = jiraIssueCreator;
  }

  public void execute(Context context) {
    checkConditions(context.projectSettings());
    createPolarionIssue(context);
  }

  protected void createPolarionIssue(Context context){
    WorkItem issue;
    try {
      issue = PolarionIssueCreator.createIssue(context.issue(), context.projectSettings());
    } catch (Exception e) {
      throw new IllegalStateException("Impossible to create an issue on Polarion. A problem occured with the remote server: " + e.getMessage(), e);
    }

    createComment(issue, context);
    // and add the property
    context.setAttribute(PolarionConstants.SONAR_ISSUE_DATA_PROPERTY_KEY, issue.getId());
  }

  @VisibleForTesting
  void checkConditions(Settings settings) {
    checkProperty(PolarionConstants.SERVER_URL_PROPERTY, settings);
//    checkProperty(PolarionConstants.SOAP_BASE_URL_PROPERTY, settings);
    checkProperty(PolarionConstants.POLARION_USERNAME_PROPERTY, settings);
    checkProperty(PolarionConstants.POLARION_PASSWORD_PROPERTY, settings);
/*    checkProperty(PolarionConstants.POLARION_PROJECT_KEY_PROPERTY, settings);
    checkProperty(PolarionConstants.POLARION_INFO_PRIORITY_ID, settings);
    checkProperty(PolarionConstants.POLARION_MINOR_PRIORITY_ID, settings);
    checkProperty(PolarionConstants.POLARION_MAJOR_PRIORITY_ID, settings);
    checkProperty(PolarionConstants.POLARION_CRITICAL_PRIORITY_ID, settings);
    checkProperty(PolarionConstants.POLARION_BLOCKER_PRIORITY_ID, settings);
    checkProperty(PolarionConstants.POLARION_ISSUE_TYPE_ID, settings);
    checkProperty(PolarionConstants.POLARION_ISSUE_COMPONENT_ID, settings);
*/  }

  private void checkProperty(String property, Settings settings) { //TODO
    if (!settings.hasKey(property) && !settings.hasDefaultValue(property)) {
      throw new IllegalStateException("The Polarion property \""+ property + "\" must be defined before you can use the \"Link to Polarion\" button");
    }
  }

  protected void createComment(WorkItem issue, Context context) {
    context.addComment(generateCommentText(issue, context));
  }

  protected String generateCommentText(WorkItem issue, Context context) {
    StringBuilder message = new StringBuilder();
    message.append("Issue linked to Polarion issue: ");
    message.append(context.projectSettings().getString(PolarionConstants.SERVER_URL_PROPERTY));
    message.append("/browse/");
    message.append(issue.getId());
    return message.toString();
  }

}
