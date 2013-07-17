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

package org.sonar.plugins.polarion.soap;

import javax.xml.rpc.ServiceException;

import com.polarion.alm.ws.client.WebServiceFactory;
import com.polarion.alm.ws.client.session.SessionWebService;
import com.polarion.alm.ws.client.tracker.TrackerWebService;
import com.polarion.alm.ws.client.projects.ProjectWebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

/**
 * This represents a SOAP session with Polarion including that state of being logged in or not
 */
public class PolarionSession {
  private static final Logger LOG = LoggerFactory.getLogger(PolarionSession.class);

  private WebServiceFactory factory;
  private SessionWebService sessionService;
  private TrackerWebService trackerService;
  private ProjectWebService projectService;

  public PolarionSession(String serverAdress) throws MalformedURLException{
      String polarionServices = serverAdress+ "/polarion/ws/services/";
      factory = new WebServiceFactory(polarionServices);
      LOG.info("SOAP Session service endpoint at " + polarionServices);
  }

  public void connect(String userName, String password) throws ServiceException, RemoteException {
    LOG.debug("Connnecting via SOAP as : {}", userName);

    sessionService = factory.getSessionService();
    trackerService = factory.getTrackerService();
    projectService = factory.getProjectService();

    sessionService.logIn(userName, password);

    LOG.debug("Connected to Polarion Server");
  }

  public void disconnect() throws RemoteException {
    sessionService.endSession();
  }

  public TrackerWebService getTrackerService() {
    return trackerService;
  }

  public ProjectWebService getProjectService() {
    return projectService;
  }
}
