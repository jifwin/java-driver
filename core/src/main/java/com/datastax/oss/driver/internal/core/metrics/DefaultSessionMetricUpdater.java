/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.driver.internal.core.metrics;

import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.config.CoreDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigProfile;
import com.datastax.oss.driver.api.core.metrics.CoreSessionMetric;
import com.datastax.oss.driver.api.core.metrics.SessionMetric;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import java.time.Duration;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSessionMetricUpdater extends MetricUpdaterBase<SessionMetric>
    implements SessionMetricUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSessionMetricUpdater.class);

  private final String metricNamePrefix;

  public DefaultSessionMetricUpdater(
      Set<SessionMetric> enabledMetrics, InternalDriverContext context) {
    super(enabledMetrics, context.metricRegistry());
    this.metricNamePrefix = context.sessionName() + ".";

    if (enabledMetrics.contains(CoreSessionMetric.cql_requests)) {
      initializeCqlRequestsTimer(context.config().getDefaultProfile(), context.sessionName());
    }
  }

  @Override
  protected String buildFullName(SessionMetric metric) {
    return metricNamePrefix + metric.name();
  }

  private void initializeCqlRequestsTimer(DriverConfigProfile config, String logPrefix) {
    Duration highestLatency =
        config.getDuration(CoreDriverOption.METRICS_SESSION_CQL_REQUESTS_HIGHEST);
    final int significantDigits;
    int d = config.getInt(CoreDriverOption.METRICS_SESSION_CQL_REQUESTS_DIGITS);
    if (d >= 0 && d <= 5) {
      significantDigits = d;
    } else {
      LOG.warn(
          "[{}] Configuration option {} is out of range (expected between 0 and 5, found {}); "
              + "using 3 instead.",
          logPrefix,
          CoreDriverOption.METRICS_SESSION_CQL_REQUESTS_DIGITS,
          d);
      significantDigits = 3;
    }
    Duration refreshInterval =
        config.getDuration(CoreDriverOption.METRICS_SESSION_CQL_REQUESTS_INTERVAL);

    // Initialize eagerly
    metricRegistry.timer(
        metricNamePrefix + CoreSessionMetric.cql_requests,
        () ->
            new Timer(
                new HdrReservoir(
                    highestLatency,
                    significantDigits,
                    refreshInterval,
                    logPrefix + "." + CoreSessionMetric.cql_requests)));
  }
}
