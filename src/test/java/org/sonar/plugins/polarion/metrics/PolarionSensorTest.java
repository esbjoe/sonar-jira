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

import org.sonar.plugins.polarion.metrics.PolarionSensor.DefectPerEnumState;

import com.google.common.collect.Maps;

import com.polarion.alm.ws.client.projects.ProjectWebService;

import com.polarion.alm.ws.client.types.tracker.EnumOptionId;

import com.polarion.alm.ws.client.types.tracker.WorkItem;

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

import static org.mockito.Matchers.anyString;

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
    settings.setProperty(PolarionConstants.POLARION_FETCH_PROJECT_ID, "test1");
    settings.setProperty(PolarionConstants.POLARION_CREATE_PROJECT_ID, "test2");
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

    settings.removeProperty(PolarionConstants.POLARION_CREATE_PROJECT_ID);
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
    when(trackerService.getEnumOptionsForKeyWithControl(anyString(), anyString(), anyString())).thenReturn(new EnumOption[] {severity1});

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
    when(trackerService.getEnumOptionsForKeyWithControl(anyString(), anyString(), anyString())).thenReturn(new EnumOption[] {resolution1});

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
    when(projectService.getProject(anyString())).thenReturn(new com.polarion.alm.ws.client.types.projects.Project());
    String[] fields = { "id", "title", "severity", "priority", "status", "resolution"};
    String query="type:defect AND !resolved AND project.id:" + settings.getString(PolarionConstants.POLARION_FETCH_PROJECT_ID);
    when(trackerService.queryWorkItems(query, null, fields)).thenReturn(new WorkItem[] {issue1, issue2, issue3});

    Map<String, Integer> foundIssues = sensor.collectDefectsBySeverity(polarionSoapService, settings.getString(PolarionConstants.POLARION_FETCH_PROJECT_ID));
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
    when(projectService.getProject(anyString())).thenReturn(new com.polarion.alm.ws.client.types.projects.Project());
    String[] fields = { "id", "title", "severity", "priority", "status", "resolution"};
    String query="type:defect AND resolved AND project.id:" + settings.getString(PolarionConstants.POLARION_FETCH_PROJECT_ID);
    when(trackerService.queryWorkItems(query, null, fields)).thenReturn(new WorkItem[] {issue1, issue2, issue3});

    Map<String, Integer> foundIssues = sensor.collectDefectsByResolution(polarionSoapService, settings.getString(PolarionConstants.POLARION_FETCH_PROJECT_ID));
    assertThat(foundIssues.size()).isEqualTo(2);
    assertThat(foundIssues.get("rejected")).isEqualTo(2);
    assertThat(foundIssues.get("done")).isEqualTo(1);
  }

  @Test
  public void distributionForEmptyEnumStateShouldBeZero() throws Exception {
    Map<String, String> enumStates = Maps.newHashMap();
    enumStates.put("state1", "STATE 1");
    enumStates.put("state2", "STATE 2");
    enumStates.put("state3", "STATE 3");

    Map<String, Integer> defectsPerEnumStates = Maps.newHashMap();

    DefectPerEnumState defectPerEnumState = sensor.mapNumberOfDefectsPerEnumState(enumStates, defectsPerEnumStates);

    assertThat(defectPerEnumState.getDistribution().getProps().get("STATE 1")).isEqualTo(0);
    assertThat(defectPerEnumState.getDistribution().getProps().get("STATE 2")).isEqualTo(0);
    assertThat(defectPerEnumState.getDistribution().getProps().get("STATE 3")).isEqualTo(0);

  }

  @Test
  public void totalNumberOfDefectShallBeSet() throws Exception {
    Map<String, String> enumStates = Maps.newHashMap();
    enumStates.put("state1", "STATE 1");
    enumStates.put("state2", "STATE 2");
    enumStates.put("state3", "STATE 3");

    Map<String, Integer> defectsPerEnumStates = Maps.newHashMap();
    defectsPerEnumStates.put("state2", 10);
    defectsPerEnumStates.put("state3", 5);
    DefectPerEnumState defectPerEnumState = sensor.mapNumberOfDefectsPerEnumState(enumStates, defectsPerEnumStates);

    assertThat(defectPerEnumState.getTotalNumberOfDefects()).isEqualTo(15);
  }

  @Test
  public void nrOfDefectsPerEnumStateShallBeSet() throws Exception {
    Map<String, String> enumStates = Maps.newHashMap();
    enumStates.put("state1", "STATE 1");
    enumStates.put("state2", "STATE 2");
    enumStates.put("state3", "STATE 3");

    Map<String, Integer> defectsPerEnumStates = Maps.newHashMap();
    defectsPerEnumStates.put("state2", 10);
    defectsPerEnumStates.put("state3", 5);
    DefectPerEnumState defectPerEnumState = sensor.mapNumberOfDefectsPerEnumState(enumStates, defectsPerEnumStates);

    assertThat(defectPerEnumState.getDistribution().getProps().get("STATE 1")).isEqualTo(0);
    assertThat(defectPerEnumState.getDistribution().getProps().get("STATE 2")).isEqualTo(10);
    assertThat(defectPerEnumState.getDistribution().getProps().get("STATE 3")).isEqualTo(5);
  }

  @Test(expected=IllegalArgumentException.class)
  public void shallThrowIllegalArugemtExceptionIfPolarionProjectIsUnresolvable() throws RemoteException {
    PolarionSession service = mock(PolarionSession.class);
    ProjectWebService projectService = mock(ProjectWebService.class);
    when(service.getProjectService()).thenReturn(projectService);

    com.polarion.alm.ws.client.types.projects.Project polarionProject = new com.polarion.alm.ws.client.types.projects.Project();
    polarionProject.setUnresolvable(true);

    when(projectService.getProject(anyString())).thenReturn(polarionProject);
    String polarionProjectId = "test";
    String query = "testQuery";
    sensor.getDefectsForProject(query, service, polarionProjectId);
  }

  @Test
  public void queryPolarionForWorkItemShallReturnEmptyArrayIfNoDefectsIsFound() throws RemoteException {
    PolarionSession service = mock(PolarionSession.class);
    TrackerWebService trackerService = mock(TrackerWebService.class);
    when(service.getTrackerService()).thenReturn(trackerService);

    String[] fields = { "id", "title"};
    String query = "testQuery";
    when(trackerService.queryWorkItems(query, null, fields)).thenReturn(null);

    String polarionProjectId = "test";


    WorkItem[] workitems = sensor.queryPolarionForWorkItem(service, polarionProjectId, query, fields);

    assertThat(workitems).isEqualTo(new WorkItem[0]);
  }
}
