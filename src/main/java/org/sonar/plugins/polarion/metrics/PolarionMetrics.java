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
import org.sonar.api.measures.Metrics;

import java.util.ArrayList;
import java.util.List;

public final class PolarionMetrics implements Metrics {
  public static final String ISSUES_DOMAIN = "Issues";

  public static final String OPEN_ISSUES_KEY = "openIssues";
  public static final Metric OPENISSUES = new Metric.Builder(OPEN_ISSUES_KEY, "Polarion Unresolved Issues", Metric.ValueType.INT)
      .setDescription("Number of Unresolved Polarion Issues")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(ISSUES_DOMAIN)
      .create();

  public static final String RESOLVED_ISSUES_KEY = "resolvedIssues";
  public static final Metric RESOLVEDISSUES = new Metric.Builder(RESOLVED_ISSUES_KEY, "Polarion Resolved Issues", Metric.ValueType.INT)
      .setDescription("Number of Resolved Polarion Issues")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(ISSUES_DOMAIN)
      .create();

  public List<Metric> getMetrics() {
    List<Metric> metrics = new ArrayList<Metric>();
    metrics.add(OPENISSUES);
    metrics.add(RESOLVEDISSUES);
    return metrics;
  }

}
