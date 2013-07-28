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

import org.sonar.api.measures.Metric;

import com.polarion.alm.ws.client.types.tracker.EnumOption;
import com.polarion.alm.ws.client.projects.ProjectWebService;
import com.polarion.alm.ws.client.tracker.TrackerWebService;
import com.polarion.alm.ws.client.types.tracker.WorkItem;
import org.sonar.plugins.polarion.soap.PolarionSession;
import org.sonar.plugins.polarion.PolarionConstants;

import com.google.common.collect.Maps;

import javax.xml.rpc.ServiceException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Project;



import java.rmi.RemoteException;
import java.util.Map;

@Properties({
  @Property(
    key = PolarionConstants.POLARION_FETCH_PROJECT_ID,
    name = "Project Fetch Id",
    description = "Project ID of Polarion project where defects shall be fetched from. Case sensitive, example : elibrary",
    global = false,
    project = true,
    module = true
  )
})
public class PolarionSensor implements Sensor {
  private static final Logger LOG = LoggerFactory.getLogger(PolarionSensor.class);

  private final Settings settings;

  public PolarionSensor(Settings settings) {
    this.settings = settings;
  }

  private String getServerUrl() {
    return  settings.getString(PolarionConstants.SERVER_URL_PROPERTY);
  }

  private String getUsername() {
    return settings.getString(PolarionConstants.POLARION_USERNAME_PROPERTY);
    }

  private String getPassword() {
    return  settings.getString(PolarionConstants.POLARION_PASSWORD_PROPERTY);
  }

  private String getFetchProjectId() {
     return settings.getString(PolarionConstants.POLARION_FETCH_PROJECT_ID);
  }

  private String getCreateProjectId() {
    return settings.getString(PolarionConstants.POLARION_CREATE_PROJECT_ID);
 }

  public boolean shouldExecuteOnProject(Project project) {
    if (missingMandatoryParameters()) {
      LOG.warn("Polarion issues sensor will not run due to some parameters are missing.");
    }
    return project.isRoot() && !missingMandatoryParameters();
  }

  public void analyse(Project project, SensorContext context) {
     try {
      PolarionSession session = new PolarionSession(getServerUrl());

      session.connect(getUsername(), getPassword());

      runAnalysis(context, session, getFetchProjectId());

      session.disconnect();
      LOG.info("Disconnected from Polarion server");

    } catch (RemoteException e) {
      LOG.error("Login unsuccessful", e);
    } catch (ServiceException e) {
      LOG.error("WebServices not available", e);
    } catch (Exception e) {
      LOG.error("General exception: ",e);
    }
  }

  protected void runAnalysis(SensorContext context, PolarionSession service, String polarionProjectId) throws RemoteException {
    collectAndSaveOpenPolarionDefects(context, service, polarionProjectId);
    collectAndSaveResolvedPolarionDefects(context, service, polarionProjectId);
  }

  protected void collectAndSaveOpenPolarionDefects(SensorContext context, PolarionSession service, String polarionProjectId) throws RemoteException {
    Map<String, Integer> issuesWithSeverity = collectDefectsBySeverity(service, polarionProjectId);
    Map<String, String> severitiesEnumStates = collectSeveritiesEnumStates(service);

    DefectPerEnumState defectsPerSeverity = mapNumberOfDefectsPerEnumState(severitiesEnumStates, issuesWithSeverity);

    String url = getServerUrl() + "/polarion/#/project/" + polarionProjectId + "/workitems?query=type:defect%20AND%20NOT%20HAS_VALUE:resolution";
    LOG.debug("polarion defect url: " + url);
    saveMeasures(PolarionMetrics.OPENISSUES, context, url, defectsPerSeverity.totalNumberOfDefects, defectsPerSeverity.distribution.buildData());
  }

  protected void collectAndSaveResolvedPolarionDefects(SensorContext context, PolarionSession service, String polarionProjectId) throws RemoteException {
    Map<String, String> resolutionEnumStates = collectResolutionEnumStates(service);
    Map<String, Integer> defectsWithResolution = collectDefectsByResolution(service, polarionProjectId);

    DefectPerEnumState defectsPerResolution = mapNumberOfDefectsPerEnumState(resolutionEnumStates, defectsWithResolution);

    String url = getServerUrl() + "/polarion/#/project/" + polarionProjectId + "/workitems?query=type:defect%20AND%20HAS_VALUE:resolution";
    LOG.debug("polarion defect url: " + url);
    saveMeasures(PolarionMetrics.RESOLVEDISSUES, context, url, defectsPerResolution.totalNumberOfDefects, defectsPerResolution.distribution.buildData());
  }

  protected DefectPerEnumState mapNumberOfDefectsPerEnumState(Map<String, String> enumStates, Map<String, Integer> defectsPerEnumStates) {
    DefectPerEnumState defectPerEnumState = new DefectPerEnumState();
    for (Map.Entry<String, String> enumState : enumStates.entrySet()) {
      String enumStateId = enumState.getKey();

      if(defectsPerEnumStates.isEmpty()) {
        defectPerEnumState.addNumberOfDefectsForCurrentEnumState(enumStates.get(
            enumStateId), 0);
      }

      for(Map.Entry<String, Integer> defectsForAnEnumState : defectsPerEnumStates.entrySet()) {
        Integer nrIssues = 0;

        if(enumStateId.equals(defectsForAnEnumState.getKey())) {
          defectPerEnumState.totalNumberOfDefects += defectsForAnEnumState.getValue();
          nrIssues = defectsForAnEnumState.getValue();
          defectPerEnumState.addNumberOfDefectsForCurrentEnumState(
              enumStates.get(enumStateId), nrIssues);
         break;
        }
        defectPerEnumState.addNumberOfDefectsForCurrentEnumState(enumStates.get(
            enumStateId), nrIssues);
      }
    }
    return defectPerEnumState;
  }

  protected Map<String, Integer> collectDefectsBySeverity(PolarionSession service, String polarionProjectId) throws RemoteException {
  Map<String, Integer> issuesBySeverity = Maps.newHashMap();

  String query="type:defect AND !resolved AND project.id:" + polarionProjectId;
  WorkItem[] defects = getDefectsForProject(query, service, polarionProjectId);

  for (WorkItem defect : defects) {
    String severity = defect.getSeverity().getId();
    if (!issuesBySeverity.containsKey(severity)) {
      issuesBySeverity.put(severity, 1);
    } else {
      issuesBySeverity.put(severity, issuesBySeverity.get(severity) + 1);
    }
  }

  return issuesBySeverity;
}

  protected Map<String, Integer> collectDefectsByResolution(PolarionSession service, String polarionProjectId) throws RemoteException {
    Map<String, Integer> issuesByResolution = Maps.newHashMap();

    String query="type:defect AND resolved AND project.id:" + polarionProjectId;
    WorkItem[] defects = getDefectsForProject(query, service, polarionProjectId);

    for (WorkItem defect : defects) {
      String resolution = defect.getResolution().getId();
      if (!issuesByResolution.containsKey(resolution)) {
        issuesByResolution.put(resolution, 1);
      } else {
        issuesByResolution.put(resolution, issuesByResolution.get(resolution) + 1);
      }
    }

    return issuesByResolution;
  }

  protected WorkItem[] getDefectsForProject(String query, PolarionSession service, String polarionProjectId) throws RemoteException {

    ProjectWebService projectService = service.getProjectService();
    com.polarion.alm.ws.client.types.projects.Project polarionProject;

    polarionProject = projectService.getProject(polarionProjectId);
    if(polarionProject.isUnresolvable())
    {
      String errorText = "Polarion project id: " + polarionProjectId + " does not exist. " +
          "Please check the spelling.";
      throw new IllegalArgumentException(errorText);
    }

    LOG.debug("Polarion defect query: " + query);
    String[] fields = { "id", "title", "severity", "priority", "status", "resolution"};

    WorkItem[] defects = queryPolarionForWorkItem(service, polarionProjectId, query, fields);

    return defects;
  }

  protected WorkItem[] queryPolarionForWorkItem(PolarionSession service, String polarionProjectId, String query, String[] fields) throws RemoteException {
    TrackerWebService trackerService = service.getTrackerService();

    LOG.info("Retreive Workitems from project: " + polarionProjectId);
    LOG.debug("Query: " + query);
    WorkItem[] defects = trackerService.queryWorkItems(query, null, fields);

    if(defects == null) {
      defects = new WorkItem[0];
      LOG.warn("No Polarion defects was found for query: " + query);
    }
    else {
      LOG.info("Number of workitems found in " + polarionProjectId + ": " + defects.length);
    }

    return defects;
  }

  protected Map<String, String> collectSeveritiesEnumStates(PolarionSession service) throws RemoteException {
    Map<String, String> severities = Maps.newHashMap();
    TrackerWebService trackerService = service.getTrackerService();
    EnumOption[] allConfiguredSeverities = trackerService.getEnumOptionsForKeyWithControl(getFetchProjectId()
        , "severity", "defect");

    for ( EnumOption configuredSeverity : allConfiguredSeverities) {
      severities.put(configuredSeverity.getId(), configuredSeverity.getName());
      LOG.debug("Severity Id: " + configuredSeverity.getId() + ", name: " + configuredSeverity.getName());
    }
    return severities;
  }

  protected Map<String, String> collectResolutionEnumStates(PolarionSession service) throws RemoteException {
    Map<String, String> resolutions = Maps.newHashMap();
    TrackerWebService trackerService = service.getTrackerService();

    EnumOption[] allConfiguredResolutions = trackerService.getEnumOptionsForKeyWithControl(getFetchProjectId()
        , "resolution", "defect");

    for ( EnumOption configuredResolution : allConfiguredResolutions) {
      resolutions.put(configuredResolution.getId(), configuredResolution.getName());
      LOG.debug("Resolution Id: " + configuredResolution.getId() + ", name: " + configuredResolution.getName());
    }
    return resolutions;
  }

  protected boolean missingMandatoryParameters() {
    boolean isEmpty = StringUtils.isEmpty(getServerUrl()) ||
        StringUtils.isEmpty(getFetchProjectId()) ||
        StringUtils.isEmpty(getCreateProjectId()) ||
        StringUtils.isEmpty(getUsername()) ||
        StringUtils.isEmpty(getPassword());
    return isEmpty;
  }

  protected void saveMeasures(Metric metric, SensorContext context, String issueUrl, double totalPrioritiesCount, String priorityDistribution) {
    Measure issuesMeasure = new Measure(metric, totalPrioritiesCount);
    issuesMeasure.setUrl(issueUrl);
    LOG.debug("ISSUE URL: " + issueUrl);
    issuesMeasure.setData(priorityDistribution);
    LOG.debug("distribution: " + priorityDistribution);
    context.saveMeasure(issuesMeasure);
    LOG.debug("issueMeasure: " + issuesMeasure);
  }

  @Override
  public String toString() {
    return "Polarion issues sensor";
  }

  public class DefectPerEnumState {
    private double totalNumberOfDefects = 0;
    private PropertiesBuilder<String, Integer> distribution = new PropertiesBuilder<String, Integer>();

    public double getTotalNumberOfDefects() {
      return totalNumberOfDefects;
    }

    public void setTotalNumberOfDefects(double totalNumberOfDefects) {
      this.totalNumberOfDefects= totalNumberOfDefects;
    }

    public PropertiesBuilder<String, Integer> getDistribution() {
      return distribution;
    }

    public void addNumberOfDefectsForCurrentEnumState(String enumState, Integer numberOfIssues) {
      distribution.add(enumState, numberOfIssues);
    }
  }

}
