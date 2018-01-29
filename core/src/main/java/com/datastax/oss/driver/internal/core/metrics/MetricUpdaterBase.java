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

import com.codahale.metrics.MetricRegistry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class MetricUpdaterBase<MetricT> implements MetricUpdater<MetricT> {

  protected final Set<MetricT> enabledMetrics;
  protected final MetricRegistry metricRegistry;

  protected MetricUpdaterBase(Set<MetricT> enabledMetrics, MetricRegistry metricRegistry) {
    this.enabledMetrics = enabledMetrics;
    this.metricRegistry = metricRegistry;
  }

  protected abstract String buildFullName(MetricT metric);

  @Override
  public void incrementCounter(MetricT metric, long amount) {
    if (enabledMetrics.contains(metric)) {
      metricRegistry.counter(buildFullName(metric)).inc(amount);
    }
  }

  @Override
  public void updateHistogram(MetricT metric, long value) {
    if (enabledMetrics.contains(metric)) {
      metricRegistry.histogram(buildFullName(metric)).update(value);
    }
  }

  @Override
  public void markMeter(MetricT metric, long amount) {
    if (enabledMetrics.contains(metric)) {
      metricRegistry.meter(buildFullName(metric)).mark(amount);
    }
  }

  @Override
  public void updateTimer(MetricT metric, long duration, TimeUnit unit) {
    if (enabledMetrics.contains(metric)) {
      metricRegistry.timer(buildFullName(metric)).update(duration, unit);
    }
  }
}
