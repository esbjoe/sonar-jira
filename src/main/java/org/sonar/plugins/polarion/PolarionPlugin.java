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

package org.sonar.plugins.polarion;

import com.google.common.collect.ImmutableList;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.polarion.metrics.PolarionMetrics;
import org.sonar.plugins.polarion.metrics.PolarionSensor;
import org.sonar.plugins.polarion.metrics.PolarionWidget;
import org.sonar.plugins.polarion.reviews.PolarionActionDefinition;
import org.sonar.plugins.polarion.reviews.PolarionIssueCreator;
import org.sonar.plugins.polarion.reviews.LinkFunction;

import java.util.List;

@Properties({
  @Property(
      key = PolarionConstants.POLARION_USERNAME_PROPERTY,
      name = "Polarion User Name",
      global = true,
      project = true,
      module = false
    ),
  @Property(
      key = PolarionConstants.POLARION_PASSWORD_PROPERTY,
      name = "Polarion Password",
      global = true,
      project = true,
      module = false
      ),
  @Property(
    key = PolarionConstants.SERVER_URL_PROPERTY,
    name = "Server URL",
    description = "Example: http://polarion.kapsch.co.at",
    defaultValue = "http://polarion.kapsch.co.at",
    global = true,
    project = true,
    module = false
  )
})
public final class PolarionPlugin extends SonarPlugin {

  public List getExtensions() {
    return ImmutableList.of(
      // metrics part
      PolarionMetrics.class, PolarionSensor.class, PolarionWidget.class //, TODO activate

      // issues part
//      PolarionIssueCreator.class, LinkFunction.class, PolarionActionDefinition.class TODO activate
    );
  }
}
