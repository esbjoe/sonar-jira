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

package org.sonar.plugins.polarion.metrics;

import com.polarion.alm.ws.client.projects.ProjectWebService;

import com.polarion.alm.ws.client.types.tracker.EnumOptionId;

import com.polarion.alm.ws.client.types.tracker.WorkItem;

import org.hibernate.annotations.Any;

import com.polarion.alm.ws.client.tracker.TrackerWebService;
import com.polarion.alm.ws.client.types.tracker.EnumOption;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.test.IsMeasure;
import org.sonar.plugins.polarion.PolarionConstants;
import org.sonar.plugins.polarion.soap.PolarionSession;

import java.rmi.RemoteException;
import java.util.Map;
import java.lang.String;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PolarionSensorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PolarionSensor sensor;
  private Settings settings;

  @Before
  public void setUp() {
    settings = new Settings();
    settings.setProperty(PolarionConstants.SERVER_URL_PROPERTY, "http://my.polarion.server");
    settings.setProperty(PolarionConstants.POLARION_USERNAME_PROPERTY, "admin");
    settings.setProperty(PolarionConstants.POLARION_PASSWORD_PROPERTY, "adminPwd");
    settings.setProperty(PolarionConstants.POLARION_FETCH_PROJECT_ID, "test");
    sensor = new PolarionSensor(settings);
  }

  @Test
  public void testToString() throws Exception {
    assertThat(sensor.toString()).isEqualTo("Polarion issues sensor");
  }

  @Test
  public void testPresenceOfProperties() throws Exception {
    assertThat(sensor.missingMandatoryParameters()).isEqualTo(false);

    settings.removeProperty(PolarionConstants.POLARION_PASSWORD_PROPERTY);
    sensor = new PolarionSensor(settings);
    assertThat(sensor.missingMandatoryParameters()).isEqualTo(true);

    settings.removeProperty(PolarionConstants.POLARION_USERNAME_PROPERTY);
    sensor = new PolarionSensor(settings);
    assertThat(sensor.missingMandatoryParameters()).isEqualTo(true);

    settings.removeProperty(PolarionConstants.POLARION_FETCH_PROJECT_ID);
    sensor = new PolarionSensor(settings);
    assertThat(sensor.missingMandatoryParameters()).isEqualTo(true);

    settings.removeProperty(PolarionConstants.SERVER_URL_PROPERTY);
    sensor = new PolarionSensor(settings);
    assertThat(sensor.missingMandatoryParameters()).isEqualTo(true);
  }

  @Test
  public void shouldExecuteOnRootProjectWithAllParams() throws Exception {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(true).thenReturn(false);

    assertThat(sensor.shouldExecuteOnProject(project)).isEqualTo(true);
  }

  @Test
  public void shouldNotExecuteOnNonRootProject() throws Exception {
    assertThat(sensor.shouldExecuteOnProject(mock(Project.class))).isEqualTo(false);
  }

  @Test
  public void shouldNotExecuteOnRootProjectifOneParamMissing() throws Exception {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(true).thenReturn(false);

    settings.removeProperty(PolarionConstants.SERVER_URL_PROPERTY);
    sensor = new PolarionSensor(settings);

    assertThat(sensor.shouldExecuteOnProject(project)).isEqualTo(false);
  }

  @Test
  public void testSaveMeasures() {
    SensorContext context = mock(SensorContext.class);
    String url = "http://localhost/polarion";
    String priorityDistribution = "Critical=1";

    sensor.saveMeasures(PolarionMetrics.OPENISSUES, context, url, 1, priorityDistribution);

    verify(context).saveMeasure(argThat(new IsMeasure(PolarionMetrics.OPENISSUES, 1.0, priorityDistribution)));
    verifyNoMoreInteractions(context);
  }

  @Test
  public void shouldCollectSeveritiesEnumStates() throws Exception {
    PolarionSession polarionSoapService = mock(PolarionSession.class);
    TrackerWebService trackerService = mock(TrackerWebService.class);
    EnumOption severity1 = new EnumOption();
    severity1.setId("1");
    severity1.setName("Minor");
    when(polarionSoapService.getTrackerService()).thenReturn(trackerService);
    when(trackerService.getEnumOptionsForKeyWithControl("test", "severity", "defect")).thenReturn(new EnumOption[] {severity1});

    Map<String, String> foundSeverities = sensor.collectSeveritiesEnumStates(polarionSoapService);
    assertThat(foundSeverities.size()).isEqualTo(1);
    assertThat(foundSeverities.get("1")).isEqualTo("Minor");
  }

  @Test
  public void shouldCollectResolutionEnumStates() throws Exception {
    PolarionSession polarionSoapService = mock(PolarionSession.class);
    TrackerWebService trackerService = mock(TrackerWebService.class);
    EnumOption resolution1 = new EnumOption();
    resolution1.setId("1");
    resolution1.setName("Done");
    when(polarionSoapService.getTrackerService()).thenReturn(trackerService);
    when(trackerService.getEnumOptionsForKeyWithControl("test", "resolution", "defect")).thenReturn(new EnumOption[] {resolution1});

    Map<String, String> foundSeverities = sensor.collectResolutionEnumStates(polarionSoapService);
    assertThat(foundSeverities.size()).isEqualTo(1);
    assertThat(foundSeverities.get("1")).isEqualTo("Done");
  }

  @Test
  public void shouldCollectOpenIssuesBySeverity() throws Exception {
    PolarionSession polarionSoapService = mock(PolarionSession.class);
    TrackerWebService trackerService = mock(TrackerWebService.class);
    ProjectWebService projectService = mock(ProjectWebService.class);
    WorkItem issue1 = new WorkItem();
    issue1.setSeverity(new EnumOptionId("minor"));
    WorkItem issue2 = new WorkItem();
    issue2.setSeverity(new EnumOptionId("critical"));
    WorkItem issue3 = new WorkItem();
    issue3.setSeverity(new EnumOptionId("critical"));

    com.polarion.alm.ws.client.types.projects.Project project = new com.polarion.alm.ws.client.types.projects.Project();
    project.setUnresolvable(false);

    when(polarionSoapService.getTrackerService()).thenReturn(trackerService);
    when(polarionSoapService.getProjectService()).thenReturn(projectService);
    when(projectService.getProject("test")).thenReturn(new com.polarion.alm.ws.client.types.projects.Project());
    String[] fields = { "id", "title", "severity", "priority", "status", "resolution"};
    String query="type:defect AND !resolved AND project.id:test";
    when(trackerService.queryWorkItems(query, null, fields)).thenReturn(new WorkItem[] {issue1, issue2, issue3});

    Map<String, Integer> foundIssues = sensor.collectIssuesBySeverity(polarionSoapService, settings.getString(PolarionConstants.POLARION_FETCH_PROJECT_ID));
    assertThat(foundIssues.size()).isEqualTo(2);
    assertThat(foundIssues.get("critical")).isEqualTo(2);
    assertThat(foundIssues.get("minor")).isEqualTo(1);
  }

  @Test
  public void shouldCollectResolvedIssuesByResolution() throws Exception {
    PolarionSession polarionSoapService = mock(PolarionSession.class);
    TrackerWebService trackerService = mock(TrackerWebService.class);
    ProjectWebService projectService = mock(ProjectWebService.class);
    WorkItem issue1 = new WorkItem();
    issue1.setResolution(new EnumOptionId("done"));
    WorkItem issue2 = new WorkItem();
    issue2.setResolution(new EnumOptionId("rejected"));
    WorkItem issue3 = new WorkItem();
    issue3.setResolution(new EnumOptionId("rejected"));

    com.polarion.alm.ws.client.types.projects.Project project = new com.polarion.alm.ws.client.types.projects.Project();
    project.setUnresolvable(false);

    when(polarionSoapService.getTrackerService()).thenReturn(trackerService);
    when(polarionSoapService.getProjectService()).thenReturn(projectService);
    when(projectService.getProject("test")).thenReturn(new com.polarion.alm.ws.client.types.projects.Project());
    String[] fields = { "id", "title", "severity", "priority", "status", "resolution"};
    String query="type:defect AND resolved AND project.id:test";
    when(trackerService.queryWorkItems(query, null, fields)).thenReturn(new WorkItem[] {issue1, issue2, issue3});

    Map<String, Integer> foundIssues = sensor.collectIssuesByResolution(polarionSoapService, settings.getString(PolarionConstants.POLARION_FETCH_PROJECT_ID));
    assertThat(foundIssues.size()).isEqualTo(2);
    assertThat(foundIssues.get("rejected")).isEqualTo(2);
    assertThat(foundIssues.get("done")).isEqualTo(1);
  }
/*
  @Test
  public void shouldFindFilters() throws Exception {
    JiraSoapService jiraSoapService = mock(JiraSoapService.class);
    RemoteFilter filter1 = new RemoteFilter();
    filter1.setName("fooFilter");
    RemoteFilter myFilter = new RemoteFilter();
    myFilter.setName("myFilter");
    when(jiraSoapService.getFavouriteFilters("token")).thenReturn(new RemoteFilter[] {filter1, myFilter});

    RemoteFilter foundFilter = sensor.findJiraFilter(jiraSoapService, "token");
    assertThat(foundFilter).isEqualTo(myFilter);
  }

  @Test
  public void shouldFindFiltersWithPreviousJiraVersions() throws Exception {
    JiraSoapService jiraSoapService = mock(JiraSoapService.class);
    RemoteFilter myFilter = new RemoteFilter();
    myFilter.setName("myFilter");
    when(jiraSoapService.getSavedFilters("token")).thenReturn(new RemoteFilter[] {myFilter});
    when(jiraSoapService.getFavouriteFilters("token")).thenThrow(RemoteException.class);

    RemoteFilter foundFilter = sensor.findJiraFilter(jiraSoapService, "token");
    assertThat(foundFilter).isEqualTo(myFilter);
  }

  @Test
  public void faillIfNoFilterFound() throws Exception {
    JiraSoapService jiraSoapService = mock(JiraSoapService.class);
    when(jiraSoapService.getFavouriteFilters("token")).thenReturn(new RemoteFilter[0]);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to find filter 'myFilter' in JIRA");

    sensor.findJiraFilter(jiraSoapService, "token");
  }
*/
}
