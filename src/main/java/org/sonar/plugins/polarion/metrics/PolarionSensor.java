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
    key = PolarionConstants.POLARION_PROJECT_ID,
    name = "Project ID",
    description = "Case sensitive, example : elibrary",
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
    String serverUrl =  settings.getString(PolarionConstants.SERVER_URL_PROPERTY);
    return serverUrl;
  }

  private String getUsername() {
    String userName = settings.getString(PolarionConstants.POLARION_USERNAME_PROPERTY);
    return userName;
    }

  private String getPassword() {
    String pwd =  settings.getString(PolarionConstants.POLARION_PASSWORD_PROPERTY);
    return pwd;
  }

  private String getProjectId() {
     String polarionProjectId = settings.getString(PolarionConstants.POLARION_PROJECT_ID);
     return polarionProjectId;
  }

  public boolean shouldExecuteOnProject(Project project) {
    if (missingMandatoryParameters()) {
      LOG.error("Polarion issues sensor will not run due to some parameters are missing.");
    }
    return project.isRoot() && !missingMandatoryParameters();
  }

  public void analyse(Project project, SensorContext context) {
     try {
      PolarionSession session = new PolarionSession(getServerUrl());

      session.connect(getUsername(), getPassword());

      runAnalysis(context, session, getProjectId());

      session.disconnect();
      LOG.info("Disconnected from Polarion server");

    } catch (RemoteException e) {
      LOG.error("Login unsuccessful", e);
    } catch (ServiceException e) {
      LOG.error("WebServices not available", e);
    } catch (Exception e) {
      LOG.error(e.getMessage());
      e.printStackTrace();
    }
  }

  protected void runAnalysis(SensorContext context, PolarionSession service, String polarionProjectId) throws RemoteException {
  Map<String, Integer> issuesBySeverity = collectIssuesBySeverity(service, polarionProjectId);
  Map<String, String> priorities = collectSeveritiesEnum(service);

  double total = 0;
  PropertiesBuilder<String, Integer> distribution = new PropertiesBuilder<String, Integer>();
  for (Map.Entry<String, Integer> entry : issuesBySeverity.entrySet()) {
    total += entry.getValue();
    distribution.add(priorities.get(entry.getKey()), entry.getValue());
    LOG.debug("Distribution of collected issues: " + distribution);
  }

  String url = getServerUrl() + "/polarion/#/project/" + polarionProjectId + "/workitems/defect";
  LOG.debug("polarion defect url: " + url);
  saveMeasures(context, url, total, distribution.buildData());
}

  protected Map<String, Integer> collectIssuesBySeverity(PolarionSession service, String polarionProjectId) throws RemoteException {
  Map<String, Integer> issuesBySeverity = Maps.newHashMap();

  WorkItem[] defects = getDefectsForProject(service, polarionProjectId);

  if(defects != null) {
    for (WorkItem defect : defects) {
      String severity = defect.getSeverity().getId();
      if (!issuesBySeverity.containsKey(severity)) {
        issuesBySeverity.put(severity, 1);
      } else {
        issuesBySeverity.put(severity, issuesBySeverity.get(severity) + 1);
      }
    }
  }
  return issuesBySeverity;
}

  private WorkItem[] getDefectsForProject(PolarionSession service, String polarionProjectId) throws RemoteException {
    TrackerWebService trackerService = service.getTrackerService();
    ProjectWebService projectService = service.getProjectService();

    com.polarion.alm.ws.client.types.projects.Project polarionProject;

    polarionProject = projectService.getProject(polarionProjectId);
    if(polarionProject.isUnresolvable())
    {
      String errorText = "Polarion project id: " + polarionProjectId + " does not exist. " +
          "Please check the spelling.";
      throw new IllegalArgumentException(errorText);
    }

    //String trackerPrefix = polarionProject.getTrackerPrefix();
    //String query="type:defect AND id:" + trackerPrefix + "*"; //TODO customize query to suit production setup
    String query="type:defect AND !resolved AND " + polarionProjectId;
    LOG.debug("Polarion defect query: " + query);
    String[] fields = { "id", "title", "severity", "priority", "status", "resolution"};

    WorkItem[] defects = null;

    try {
      defects = trackerService.queryWorkItems(query, null, fields);
      LOG.info("Number of defects found in " + polarionProjectId + ": " + defects.length);
    } catch (NullPointerException ex) {
      LOG.info("Number of defects found in " + polarionProjectId + ": 0");
    }

    return defects;
  }

  protected Map<String, String> collectSeveritiesEnum(PolarionSession service) throws RemoteException {
    Map<String, String> severities = Maps.newHashMap();
    TrackerWebService trackerService = service.getTrackerService();

    EnumOption[] allConfiguredSeverities = trackerService.getEnumOptionsForKeyWithControl(getProjectId()
        , "severity", "defect");

    for ( EnumOption configuredSeverity : allConfiguredSeverities) {
      severities.put(configuredSeverity.getId(), configuredSeverity.getName());
      LOG.debug("Severity Id: " + configuredSeverity.getId() + ", name: " + configuredSeverity.getName());
    }
    return severities;
  }

  protected boolean missingMandatoryParameters() {
    LOG.debug("server URL: " + getServerUrl());
    LOG.debug("projectId: " + getProjectId());
    LOG.debug("username: " + getUsername());
    LOG.debug("password: " + getPassword());

    return StringUtils.isEmpty(getServerUrl()) ||
      StringUtils.isEmpty(getProjectId()) ||
      StringUtils.isEmpty(getUsername()) ||
      StringUtils.isEmpty(getPassword());
  }

  protected void saveMeasures(SensorContext context, String issueUrl, double totalPrioritiesCount, String priorityDistribution) {
    Measure issuesMeasure = new Measure(PolarionMetrics.ISSUES, totalPrioritiesCount);
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
}
