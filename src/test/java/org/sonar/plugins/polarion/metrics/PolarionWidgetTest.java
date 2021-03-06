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

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class PolarionWidgetTest {

  @Test
  public void testGetTemplatePathResolvedWidget() {
    String path = new PolarionResolvedIssuesWidget().getTemplatePath();
    assertThat(getClass().getResource(path)).isNotNull();
  }

  @Test
  public void testGetTemplatePathUnresolvedWidget() {
    String path = new PolarionUnresolvedIssuesWidget().getTemplatePath();
    assertThat(getClass().getResource(path)).isNotNull();
  }

  @Test
  public void testNameAndTitleResolvedWidget() throws Exception {
    PolarionResolvedIssuesWidget widget = new PolarionResolvedIssuesWidget();
    assertThat(widget.getId()).isEqualTo("polarionResolvedIssues");
    assertThat(widget.getTitle()).isEqualTo("Polarion Resolved Issues");
  }

  @Test
  public void testNameAndTitleUnResolvedWidget() throws Exception {
    PolarionUnresolvedIssuesWidget widget = new PolarionUnresolvedIssuesWidget();
    assertThat(widget.getId()).isEqualTo("polarionUnresolvedIssues");
    assertThat(widget.getTitle()).isEqualTo("Polarion Unresolved Issues");
  }
}

