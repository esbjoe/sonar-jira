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

import org.sonar.api.issue.IssueComment;

import com.polarion.alm.ws.client.types.Text;
import com.polarion.alm.ws.client.types.tracker.EnumOptionId;
import com.polarion.alm.ws.client.types.projects.Project;
import com.polarion.alm.ws.client.projects.ProjectWebService;
import com.polarion.alm.ws.client.tracker.TrackerWebService;
import com.polarion.alm.ws.client.types.tracker.WorkItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.*;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.plugins.polarion.PolarionConstants;
import org.sonar.plugins.polarion.soap.PolarionSession;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;
/**
 * SOAP client class that is used for creating issues on a POLARION server
 */
public class PolarionIssueCreator implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(PolarionIssueCreator.class);
  private final RuleFinder ruleFinder;

  public PolarionIssueCreator(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  public String createIssue(Issue sonarIssue, Settings settings) throws RemoteException, Exception {
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
      LOG.error("The Polarion server URL is not a valid one: " + polarionUrl, e);
      throw new IllegalStateException("The Polarion server URL is not a valid one: " + polarionUrl, e);
    }
    return soapSession;
  }

  protected String doCreateIssue(Issue sonarIssue, PolarionSession soapSession, Settings settings) throws Exception{
      // Connect to Polarion ALM
    String userName = settings.getString(PolarionConstants.POLARION_USERNAME_PROPERTY);
    String password = settings.getString(PolarionConstants.POLARION_PASSWORD_PROPERTY);
    String polarionUrl = settings.getString(PolarionConstants.SERVER_URL_PROPERTY);
    try {
      soapSession.connect(userName, password);
    } catch (RemoteException e) {
      throw new IllegalStateException("Impossible to connect to the Polarion server (" + polarionUrl + "). Please check provided login credentails", e);
    }
    LOG.info("Connected to Polarion server");

    // Create the issue
    ProjectWebService projectService = soapSession.getProjectService();
    WorkItem issueToBeCreated = initPolarionIssue(projectService, sonarIssue, settings);
    TrackerWebService trackerService = soapSession.getTrackerService();
    String wiUri = trackerService.createWorkItem(issueToBeCreated);
    WorkItem createdDefect = trackerService.getWorkItemByUri(wiUri);
    String defectId = createdDefect.getId();
    LOG.debug("Successfully created issue {}", defectId);
    return defectId;
  }

  protected WorkItem initPolarionIssue(ProjectWebService projectService, Issue sonarIssue,
      Settings settings) throws RemoteException {
    String polarionProjectId = settings.getString(PolarionConstants.POLARION_PROJECT_ID);
    WorkItem issue = new WorkItem();

    Project polarionProject = projectService.getProject(polarionProjectId);
    if(polarionProject.isUnresolvable()) {
      throw new RemoteException("project id: " + polarionProjectId + "could not be located");
    }
    issue.setProject(polarionProject);
    issue.setType(new EnumOptionId("defect"));
    //issue.setPriority(sonarSeverityToJiraPriorityId(RulePriority.valueOfString(sonarIssue.severity()), settings)); //TODO
    issue.setTitle(generateIssueSummary(sonarIssue));
    issue.setDescription(generateIssueDescription(sonarIssue, settings));
    return issue;
  }

  protected String generateIssueSummary(Issue sonarIssue) {
    Rule rule = ruleFinder.findByKey(sonarIssue.ruleKey());

    StringBuilder summary = new StringBuilder("Sonar Issue #");
    summary.append(sonarIssue.key());
    LOG.debug("sonar issue key: " + sonarIssue.key());
    if (rule.getName() != null) {
      summary.append(" - ");
      summary.append(rule.getName().toString());
      LOG.debug("rule Name: " + rule.getName());
    }
    return summary.toString();
  }

  protected Text generateIssueDescription(Issue sonarIssue, Settings settings) {
    StringBuilder description = new StringBuilder("Issue detail:");
    description.append(sonarIssue.message());

    description.append("\n\nComments:");
    List<IssueComment> issueComments = sonarIssue.comments();
    for(IssueComment issueComment : issueComments) {
      description.append("\n" + issueComment.markdownText() +
          ", Added by: " + issueComment.userLogin() +
          ", Date: "+ issueComment.createdAt());
    }

    description.append("\n\nCheck it on Sonar: ");
    description.append(settings.getString(CoreProperties.SERVER_BASE_URL));
    description.append("/issue/show/");
    description.append(sonarIssue.key());
    return new Text("text/plain", description.toString(), false);
  }
}

