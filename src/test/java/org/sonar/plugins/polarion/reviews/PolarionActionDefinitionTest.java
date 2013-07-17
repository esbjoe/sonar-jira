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

import org.junit.Test;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.action.Actions;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PolarionActionDefinitionTest {

  @Test
  public void check_start() throws Exception {
    Actions actions = new Actions();
    LinkFunction function = mock(LinkFunction.class);

    PolarionActionDefinition builder = new PolarionActionDefinition(actions, function);
    builder.start();

    Action action = actions.list().get(0);
    assertThat(action.key()).isEqualTo("link-to-polarion");
    assertThat(action.functions().get(0)).isEqualTo(function);
    assertThat(action.conditions()).isNotEmpty();
  }
}

