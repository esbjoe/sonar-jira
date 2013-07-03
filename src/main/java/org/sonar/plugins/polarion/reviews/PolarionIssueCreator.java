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

//import com.atlassian.jira.rpc.soap.client.*;
import com.polarion.alm.ws.client.types.tracker.EnumOptionId;

import com.polarion.alm.ws.client.types.projects.Project;

import com.polarion.alm.ws.client.projects.ProjectWebService;

import com.polarion.alm.ws.client.tracker.TrackerWebService;

import com.polarion.alm.ws.client.types.tracker.WorkItem;

import com.polarion.alm.ws.client.session.SessionWebService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.*;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.polarion.PolarionConstants;
import org.sonar.plugins.polarion.soap.PolarionSession;
//import org.sonar.plugins.polarion.soap.JiraSoapSession;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * SOAP client class that is used for creating issues on a POLARION server
 */
/*@Properties({
  @Property(
    key = PolarionConstants.SOAP_BASE_URL_PROPERTY,
    defaultValue = PolarionConstants.SOAP_BASE_URL_DEF_VALUE,
    name = "SOAP base URL",
    description = "Base URL for the SOAP API of the POLARION server",
    global = true,
    project = true
  ),
  @Property(
    key = PolarionConstants.POLARION_PROJECT_KEY_PROPERTY,
    defaultValue = "",
    name = "POLARION project key",
    description = "Key of the POLARION project on which the issues should be created.",
    global = false,
    project = true
  ),
  @Property(
    key = PolarionConstants.POLARION_INFO_PRIORITY_ID,
    defaultValue = "5",
    name = "POLARION priority id for INFO",
    description = "POLARION priority id used to create issues for Sonar violations with severity INFO. Default is 5 (Trivial).",
    global = true,
    project = true,
    type = PropertyType.INTEGER
  ),
  @Property(
    key = PolarionConstants.POLARION_MINOR_PRIORITY_ID,
    defaultValue = "4",
    name = "POLARION priority id for MINOR",
    description = "POLARION priority id used to create issues for Sonar violations with severity MINOR. Default is 4 (Minor).",
    global = true,
    project = true,
    type = PropertyType.INTEGER
  ),
  @Property(
    key = PolarionConstants.POLARION_MAJOR_PRIORITY_ID,
    defaultValue = "3",
    name = "POLARION priority id for MAJOR",
    description = "POLARION priority id used to create issues for Sonar violations with severity MAJOR. Default is 3 (Major).",
    global = true,
    project = true,
    type = PropertyType.INTEGER
  ),
  @Property(
    key = PolarionConstants.POLARION_CRITICAL_PRIORITY_ID,
    defaultValue = "2",
    name = "POLARION priority id for CRITICAL",
    description = "POLARION priority id used to create issues for Sonar violations with severity CRITICAL. Default is 2 (Critical).",
    global = true,
    project = true,
    type = PropertyType.INTEGER
  ),
  @Property(
    key = PolarionConstants.POLARION_BLOCKER_PRIORITY_ID,
    defaultValue = "1",
    name = "POLARION priority id for BLOCKER",
    description = "POLARION priority id used to create issues for Sonar violations with severity BLOCKER. Default is 1 (Blocker).",
    global = true,
    project = true,
    type = PropertyType.INTEGER
  ),
  @Property(
    key = PolarionConstants.POLARION_ISSUE_TYPE_ID,
    defaultValue = "3",
    name = "Id of POLARION issue type",
    description = "POLARION issue type id used to create issues for Sonar violations. Default is 3 (= Task in a default POLARION installation).",
    global = true,
    project = true,
    type = PropertyType.INTEGER
  ),
  @Property(
    key = PolarionConstants.POLARION_ISSUE_COMPONENT_ID,
    defaultValue = PolarionConstants.POLARION_ISSUE_COMPONENT_ID_BLANK,
    name = "Id of POLARION component",
    description = "POLARION component id used to create issues for Sonar violations. By default no component is set.",
    global = false,
    project = true,
    type = PropertyType.INTEGER
  )
})
*/public class PolarionIssueCreator implements ServerExtension {

  private static final String QUOTE = "\n{quote}\n";
  private static final Logger LOG = LoggerFactory.getLogger(PolarionIssueCreator.class);
  private final RuleFinder ruleFinder;

  public PolarionIssueCreator(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  public WorkItem createIssue(Issue sonarIssue, Settings settings) throws RemoteException, Exception {
    PolarionSession soapSession = createSoapSession(settings);
    return doCreateIssue(sonarIssue, soapSession, settings);
  }

  protected PolarionSession createSoapSession(Settings settings) {
    String polarionUrl = settings.getString(PolarionConstants.SERVER_URL_PROPERTY);

    PolarionSession soapSession= null;
    try {
      soapSession = new PolarionSession(polarionUrl);
    }
    catch (MalformedURLException e) {
      LOG.error("The POLARION server URL is not a valid one: " + polarionUrl, e);
      throw new IllegalStateException("The POLARION server URL is not a valid one: " + polarionUrl, e);
    }
    return soapSession;
  }

  protected WorkItem doCreateIssue(Issue sonarIssue, PolarionSession soapSession, Settings settings) throws Exception{
      // Connect to Polarion ALM
    String userName = settings.getString(PolarionConstants.POLARION_USERNAME_PROPERTY);
    String password = settings.getString(PolarionConstants.POLARION_PASSWORD_PROPERTY);
    String polarionUrl = settings.getString(PolarionConstants.SERVER_URL_PROPERTY);
    String polarionProjectName = settings.getString(PolarionConstants.POLARION_PROJECT_ID);
    try {
      soapSession.connect(userName, password);
    } catch (RemoteException e) {
      throw new IllegalStateException("Impossible to connect to the POLARION server (" + polarionUrl + ").", e);
    }
    LOG.info("Connected to Polarion server");

    // And create the issue
    TrackerWebService trackerService = soapSession.getTrackerService();

    WorkItem wi = new WorkItem();
    EnumOptionId type = new EnumOptionId("defect");
    wi.setType(type);

    ProjectWebService projectService = soapSession.getProjectService();
    Project polarionProject = projectService.getProject(polarionProjectName); //TODO add check if project do not exist
    wi.setProject(polarionProject);
    String wiId = trackerService.createWorkItem(wi); //TODO test!!!!
    WorkItem createdDefect = trackerService.getWorkItemById(polarionProjectName, wiId);
//    String issueKey = returnedIssue.getKey();
    LOG.debug("Successfully created issue {}", wiId);
    return createdDefect;
  }



  protected String generateIssueSummary(Issue sonarIssue) {
    Rule rule = ruleFinder.findByKey(sonarIssue.ruleKey());

    StringBuilder summary = new StringBuilder("Sonar Issue #");
    summary.append(sonarIssue.key());
    if (rule.getName() != null) {
      summary.append(" - ");
      summary.append(rule.getName().toString());
    }
    return summary.toString();
  }

  protected String generateIssueDescription(Issue sonarIssue, Settings settings) {
    StringBuilder description = new StringBuilder("Issue detail:");
    description.append(QUOTE);
    description.append(sonarIssue.message());
    description.append(QUOTE);
    description.append("\n\nCheck it on Sonar: ");
    description.append(settings.getString(CoreProperties.SERVER_BASE_URL));
    description.append("/issue/show/");
    description.append(sonarIssue.key());
    return description.toString();
  }

  /*protected String sonarSeverityToJiraPriorityId(RulePriority reviewSeverity, Settings settings) {
    final String priorityId;
    switch (reviewSeverity) {
      case INFO:
        priorityId = settings.getString(PolarionConstants.POLARION_INFO_PRIORITY_ID);
        break;
      case MINOR:
        priorityId = settings.getString(PolarionConstants.POLARION_MINOR_PRIORITY_ID);
        break;
      case MAJOR:
        priorityId = settings.getString(PolarionConstants.POLARION_MAJOR_PRIORITY_ID);
        break;
      case CRITICAL:
        priorityId = settings.getString(PolarionConstants.POLARION_CRITICAL_PRIORITY_ID);
        break;
      case BLOCKER:
        priorityId = settings.getString(PolarionConstants.POLARION_BLOCKER_PRIORITY_ID);
        break;
      default:
        throw new SonarException("Unable to convert review severity to POLARION priority: " + reviewSeverity);
    }
    return priorityId;
  }*/
}

