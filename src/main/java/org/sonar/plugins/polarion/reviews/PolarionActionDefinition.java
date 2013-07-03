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

import org.sonar.api.ServerExtension;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.issue.condition.HasIssuePropertyCondition;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.issue.condition.NotCondition;
import org.sonar.plugins.polarion.PolarionConstants;

public final class PolarionActionDefinition implements ServerExtension {

  private static final String LINK_TO_POLARION_ID = "link-to-polarion";
  private final Actions actions;
  private final LinkFunction linkFunction;

  public PolarionActionDefinition(Actions actions, LinkFunction linkFunction) {
    this.actions = actions;
    this.linkFunction = linkFunction;
  }

  public void start() {
    actions.add(LINK_TO_POLARION_ID)
      .setConditions(
        new NotCondition(new HasIssuePropertyCondition(PolarionConstants.SONAR_ISSUE_DATA_PROPERTY_KEY)),
        new IsUnResolved()
      )
      .setFunctions(linkFunction);
  }
}

