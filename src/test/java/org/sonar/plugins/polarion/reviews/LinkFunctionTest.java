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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.action.Function;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.plugins.polarion.PolarionConstants;

import java.rmi.RemoteException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class LinkFunctionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private LinkFunction function;
  private PolarionIssueCreator polarionIssueCreator;
  private Issue sonarIssue;
  private Function.Context context;
  private Settings settings;

  @Before
  public void init() throws Exception {
    sonarIssue = new DefaultIssue().setKey("ABCD");
    settings = new Settings();

    context = mock(Function.Context.class);
    when(context.issue()).thenReturn(sonarIssue);
    when(context.projectSettings()).thenReturn(settings);

    polarionIssueCreator = mock(PolarionIssueCreator.class);

    function = new LinkFunction(polarionIssueCreator);
  }

  @Test
  public void should_execute() throws Exception {
    function.createPolarionIssue(context);

    verify(polarionIssueCreator).createIssue(sonarIssue, settings);
    verify(context).addComment(anyString());
  }

  @Test
  public void should_fail_execute_if_remote_problem() throws Exception {
    when(polarionIssueCreator.createIssue(sonarIssue, settings)).thenThrow(new RemoteException("Server Error"));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Impossible to create an issue on Polarion. A problem occured with the remote server: Server Error");

    function.createPolarionIssue(context);
  }

  @Test
  public void test_create_comment() throws Exception {
    settings.setProperty(PolarionConstants.SERVER_URL_PROPERTY, "http://my.polarion.server");

    function.createComment("FOO-15", context);

    verify(context).addComment(
        "Issue linked to Polarion issue: http://my.polarion.server/polarion/#/project/" +
            settings.getString(PolarionConstants.POLARION_CREATE_PROJECT_ID) + "/workitem?id=FOO-15");
  }

    @Test
  public void test_generate_comment_text() throws Exception {
    settings.setProperty(PolarionConstants.SERVER_URL_PROPERTY, "http://my.polarion.server");

    String commentText = function.generateCommentText("FOO-15", context);
    assertThat(commentText).isEqualTo("Issue linked to Polarion issue: http://my.polarion.server/polarion/#/project/" +
        settings.getString(PolarionConstants.POLARION_CREATE_PROJECT_ID) + "/workitem?id=FOO-15");
  }

  @Test
  public void should_check_settings() {
    settings.setProperty(PolarionConstants.SERVER_URL_PROPERTY, "http://my.polarion.server");
    settings.setProperty(PolarionConstants.POLARION_USERNAME_PROPERTY, "john");
    settings.setProperty(PolarionConstants.POLARION_PASSWORD_PROPERTY, "1234");
    settings.setProperty(PolarionConstants.POLARION_CREATE_PROJECT_ID, "SONAR");
    settings.setProperty(PolarionConstants.POLARION_FETCH_PROJECT_ID, "SONAR2");

    function.checkConditions(settings);
  }

  @Test
  public void should_fail_if_settings_is_empty() {
    try {
      function.checkConditions(settings);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }
}

