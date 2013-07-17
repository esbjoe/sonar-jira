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


import org.sonar.api.issue.internal.DefaultIssueComment;

import com.polarion.alm.ws.client.types.projects.User;

import com.polarion.alm.ws.client.types.tracker.Comment;

import com.polarion.alm.ws.client.types.Text;

import com.polarion.alm.ws.client.types.tracker.EnumOptionId;

import com.polarion.alm.ws.client.types.projects.Project;

import com.polarion.alm.ws.client.projects.ProjectWebService;
import com.polarion.alm.ws.client.tracker.TrackerWebService;
import com.polarion.alm.ws.client.types.tracker.WorkItem;
import org.sonar.plugins.polarion.soap.PolarionSession;
import org.sonar.plugins.polarion.PolarionConstants;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleFinder;
import org.sonar.plugins.polarion.PolarionPlugin;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolarionIssueCreatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private PolarionIssueCreator polarionIssueCreator;
  private Issue sonarIssue;
  private DefaultIssueComment defaultComment;
  private Settings settings;
  private RuleFinder ruleFinder;

  @Before
  public void init() throws Exception {
    defaultComment = new DefaultIssueComment();
    defaultComment.setUserLogin("admin");
    defaultComment.setMarkdownText("remove code");
    Date commentCreatedDate = new Date();
    defaultComment.setCreatedAt(commentCreatedDate);

    sonarIssue = new DefaultIssue()
      .setKey("ABCD")
      .setMessage("The Cyclomatic Complexity of this method is 14 which is greater than 10 authorized.")
      .setSeverity("MINOR")
      .addComment(defaultComment)
      .setRuleKey(RuleKey.of("squid", "CycleBetweenPackages"));

    ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(RuleKey.of("squid", "CycleBetweenPackages"))).thenReturn(org.sonar.api.rules.Rule.create().setName("Avoid cycle between java packages"));

    settings = new Settings(new PropertyDefinitions(PolarionIssueCreator.class, PolarionPlugin.class));
    settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://my.sonar.com");
    settings.setProperty(PolarionConstants.SERVER_URL_PROPERTY, "http://my.polarion.com");
    settings.setProperty(PolarionConstants.POLARION_USERNAME_PROPERTY, "foo");
    settings.setProperty(PolarionConstants.POLARION_PASSWORD_PROPERTY, "bar");
    settings.setProperty(PolarionConstants.POLARION_CREATE_PROJECT_ID, "TEST");

    polarionIssueCreator = new PolarionIssueCreator(ruleFinder);
  }

/*  @Test
  public void shouldCreateSoapSession() throws Exception {
    PolarionSession soapSession = polarionIssueCreator.createSoapSession(settings);
    assertThat(soapSession.).isEqualTo("http://my.polarion.com/rpc/soap/jirasoapservice-v2");
  }
*/
  @Test
  public void shouldFailToCreateSoapSessionWithIncorrectUrl() throws Exception {
    settings.removeProperty(PolarionConstants.SERVER_URL_PROPERTY);
    settings.appendProperty(PolarionConstants.SERVER_URL_PROPERTY, "my.server");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The Polarion server URL is not a valid one: my.server");

    polarionIssueCreator.createSoapSession(settings);
  }

  @Test
  public void shouldFailToCreateIssueIfCantConnect() throws Exception {
    // Given that
    PolarionSession soapSession = mock(PolarionSession.class);
    doThrow(RemoteException.class).when(soapSession).connect(anyString(), anyString());

    // Verify
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to connect to the Polarion server");

    polarionIssueCreator.doCreateIssue(sonarIssue, soapSession, settings);
  }

/*  @Test
  public void shouldFailToCreateIssueIfNotEnoughRights() throws Exception {
    // Given that
    PolarionSession polarionService = mock(PolarionSession.class);
    doThrow(RemotePermissionException.class).when(polarionService).createIssue(anyString(), any(RemoteIssue.class));

    // Verify
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to create the issue on the JIRA server (my.jira) because user foo does not have enough rights.");

    polarionIssueCreator.sendRequest(jiraSoapService, "", null, "my.jira", "foo");
  }
*/
/*  @Test
  public void shouldFailToCreateIssueIfRemoteError() throws Exception {
    // Given that
//    JiraSoapService jiraSoapService = mock(JiraSoapService.class);
//    doThrow(RemoteException.class).when(jiraSoapService).createIssue(anyString(), any(RemoteIssue.class));

    // Verify
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to create the issue on the JIRA server (my.jira)");

//    jiraIssueCreator.sendRequest(jiraSoapService, "", null, "my.jira", "foo");
  }
*/
  @Test
  public void shouldCreateIssue() throws Exception {
    // Given that
    PolarionSession polarionSoapService = mock(PolarionSession.class);
    TrackerWebService trackerService = mock(TrackerWebService.class);
    ProjectWebService projectService = mock(ProjectWebService.class);
    when(polarionSoapService.getProjectService()).thenReturn(projectService);
    Project polarionProject = mock(Project.class);
    polarionProject.setId("test");
    when(projectService.getProject(anyString())).thenReturn(polarionProject);
    when(polarionSoapService.getTrackerService()).thenReturn(trackerService);
    when(trackerService.createWorkItem(any(WorkItem.class))).thenReturn("hejsan");
    WorkItem wi = new WorkItem();
    wi.setId("wiId");
    when(trackerService.getWorkItemByUri(anyString())).thenReturn(wi);

    // Verify
    String returnedIssue = polarionIssueCreator.doCreateIssue(sonarIssue, polarionSoapService, settings);


    verify(polarionSoapService).connect(anyString(), anyString());

    assertThat(returnedIssue).isEqualTo(wi.getId());
  }

/*  @Test
  public void shouldInitRemoteIssue() throws Exception {
    // Given that
    ProjectWebService projectService = mock(ProjectWebService.class);
    Project expectedProject = mock(Project.class);
    when(projectService.getProject(anyString())).thenReturn(expectedProject);
    WorkItem expectedIssue = new WorkItem();
    expectedIssue.setTitle("Sonar Issue #ABCD - Avoid cycle between java packages");
    Text text = new Text("text/plain", "Issue detail: The Cyclomatic Complexity of this method is 14 which is greater than 10 authorized.\n\nCheck it on Sonar: http://my.sonar.com/issue/show/ABCD", false);
    expectedIssue.setDescription(text);

    // Verify
    WorkItem returnedIssue = polarionIssueCreator.initPolarionIssue(projectService, sonarIssue, settings);

    assertThat(returnedIssue.getTitle()).isEqualTo(expectedIssue.getTitle());
    assertThat(returnedIssue.getDescription()).isEqualTo(expectedIssue.getDescription());
  }

  */@Test
  public void shouldInitRemoteIssueWithType() throws Exception {
    // Given that
 // Given that
    ProjectWebService projectService = mock(ProjectWebService.class);
    Project expectedProject = mock(Project.class);
    when(projectService.getProject(anyString())).thenReturn(expectedProject);
    WorkItem expectedIssue = new WorkItem();
    expectedIssue.setType(new EnumOptionId("defect"));

    // Verify
    WorkItem returnedIssue = polarionIssueCreator.initPolarionIssue(projectService, sonarIssue, settings);

    assertThat(returnedIssue.getType()).isEqualTo(expectedIssue.getType());
  }

/*  @Test
  public void shouldInitRemoteIssueWithComment() throws Exception {
    // Given that

    ProjectWebService projectService = mock(ProjectWebService.class);
    Project expectedProject = mock(Project.class);
    when(projectService.getProject(anyString())).thenReturn(expectedProject);
    WorkItem expectedIssue = new WorkItem();
    Comment[] expectedComments = {new Comment()};
    User user = new User();
    user.setName(defaultComment.userLogin());
    expectedComments[0].setAuthor(user);
    Calendar cal=Calendar.getInstance();
    cal.setTime(defaultComment.createdAt());
    expectedComments[0].setCreated(cal);
    Text content = new Text();
    content.setContent(defaultComment.markdownText());
    expectedComments[0].setText(content);
    expectedComments[0].setTitle("SonarQube Review Comment");
    expectedIssue.setComments(expectedComments);


    // Verify
    WorkItem returnedIssue = polarionIssueCreator.initPolarionIssue(projectService, sonarIssue, settings);

    assertThat(returnedIssue.getComments()).isEqualTo(expectedIssue.getComments());
  }
 */ /*
  @Test
  public void shouldInitRemoteIssueWithComponent() throws Exception {
    // Given that
    settings.setProperty(PolarionConstants.JIRA_ISSUE_COMPONENT_ID, "123");
//    RemoteIssue expectedIssue = new RemoteIssue();
//    expectedIssue.setProject("TEST");
//    expectedIssue.setType("3");
//    expectedIssue.setPriority("4");
//    expectedIssue.setSummary("Sonar Issue #ABCD - Avoid cycle between java packages");
//    expectedIssue.setDescription("Issue detail:\n{quote}\nThe Cyclomatic Complexity of this method is 14 which is greater than 10 authorized.\n" +
//      "{quote}\n\n\nCheck it on Sonar: http://my.sonar.com/issue/show/ABCD");
//    expectedIssue.setComponents(new RemoteComponent[] {new RemoteComponent("123", null)});

    // Verify
//    RemoteIssue returnedIssue = jiraIssueCreator.initRemoteIssue(sonarIssue, settings);

//    assertThat(returnedIssue).isEqualTo(expectedIssue);
  }

  @Test
  public void shouldGiveDefaultPriority() throws Exception {
    assertThat(jiraIssueCreator.sonarSeverityToJiraPriorityId(RulePriority.BLOCKER, settings)).isEqualTo("1");
    assertThat(jiraIssueCreator.sonarSeverityToJiraPriorityId(RulePriority.CRITICAL, settings)).isEqualTo("2");
    assertThat(jiraIssueCreator.sonarSeverityToJiraPriorityId(RulePriority.MAJOR, settings)).isEqualTo("3");
    assertThat(jiraIssueCreator.sonarSeverityToJiraPriorityId(RulePriority.MINOR, settings)).isEqualTo("4");
    assertThat(jiraIssueCreator.sonarSeverityToJiraPriorityId(RulePriority.INFO, settings)).isEqualTo("5");
  }

  @Test
  public void shouldInitRemoteIssueWithoutName() throws Exception {
    // Given that
    when(ruleFinder.findByKey(RuleKey.of("squid", "CycleBetweenPackages"))).thenReturn(org.sonar.api.rules.Rule.create().setName(null));

//    RemoteIssue expectedIssue = new RemoteIssue();
//    expectedIssue.setProject("TEST");
//    expectedIssue.setType("3");
//    expectedIssue.setPriority("4");
//    expectedIssue.setSummary("Sonar Issue #ABCD");
//    expectedIssue.setDescription("Issue detail:\n{quote}\nThe Cyclomatic Complexity of this method is 14 which is greater than 10 authorized.\n" +
//      "{quote}\n\n\nCheck it on Sonar: http://my.sonar.com/issue/show/ABCD");

    // Verify
//    RemoteIssue returnedIssue = jiraIssueCreator.initRemoteIssue(sonarIssue, settings);

//    assertThat(returnedIssue.getSummary()).isEqualTo(expectedIssue.getSummary());
//    assertThat(returnedIssue.getDescription()).isEqualTo(expectedIssue.getDescription());
//    assertThat(returnedIssue).isEqualTo(expectedIssue);
  }
*/}

