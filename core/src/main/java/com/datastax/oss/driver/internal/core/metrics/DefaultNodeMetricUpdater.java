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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.config.CoreDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigProfile;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metrics.CoreNodeMetric;
import com.datastax.oss.driver.api.core.metrics.NodeMetric;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import com.datastax.oss.driver.internal.core.pool.ChannelPool;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultNodeMetricUpdater extends MetricUpdaterBase<NodeMetric>
    implements NodeMetricUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultNodeMetricUpdater.class);

  private final String metricNamePrefix;

  public DefaultNodeMetricUpdater(
      Node node, Set<NodeMetric> enabledMetrics, InternalDriverContext context) {
    super(enabledMetrics, context.metricRegistry());
    this.metricNamePrefix = buildPrefix(context.sessionName(), node.getConnectAddress());

    String logPrefix = context.sessionName();
    DriverConfigProfile config = context.config().getDefaultProfile();

    if (enabledMetrics.contains(CoreNodeMetric.pooled_connection_count)) {
      metricRegistry.register(
          metricNamePrefix + CoreNodeMetric.pooled_connection_count.name(),
          (Gauge<Integer>)
              () -> {
                ChannelPool pool = context.poolManager().getPools().get(node);
                return (pool == null) ? 0 : pool.size();
              });
    }
    if (enabledMetrics.contains(CoreNodeMetric.available_stream_count)) {
      metricRegistry.register(
          metricNamePrefix + CoreNodeMetric.available_stream_count,
          (Gauge<Integer>)
              () -> {
                ChannelPool pool = context.poolManager().getPools().get(node);
                return (pool == null) ? 0 : pool.getAvailableIds();
              });
    }
    if (enabledMetrics.contains(CoreNodeMetric.cql_messages)) {
      initializeCqlMessagesTimer(config, logPrefix);
    }
  }

  @Override
  protected String buildFullName(NodeMetric metric) {
    return metricNamePrefix + metric.name();
  }

  private String buildPrefix(String sessionName, InetSocketAddress addressAndPort) {
    StringBuilder prefix = new StringBuilder(sessionName).append(".nodes.");
    InetAddress address = addressAndPort.getAddress();
    int port = addressAndPort.getPort();
    if (address instanceof Inet4Address) {
      // Metrics use '.' as a delimiter, replace so that the IP is a single path component
      // (127.0.0.1 => 127_0_0_1)
      prefix.append(address.getHostAddress().replace('.', '_'));
    } else {
      assert address instanceof Inet6Address;
      // IPv6 only uses '%' and ':' as separators, so no replacement needed
      prefix.append(address.getHostAddress());
    }
    // Append the port in anticipation of when C* will support nodes on different ports
    return prefix.append('_').append(port).append('.').toString();
  }

  private void initializeCqlMessagesTimer(DriverConfigProfile config, String logPrefix) {
    Duration highestLatency =
        config.getDuration(CoreDriverOption.METRICS_NODE_CQL_MESSAGES_HIGHEST);
    final int significantDigits;
    int d = config.getInt(CoreDriverOption.METRICS_NODE_CQL_MESSAGES_DIGITS);
    if (d >= 0 && d <= 5) {
      significantDigits = d;
    } else {
      LOG.warn(
          "[{}] Configuration option {} is out of range (expected between 0 and 5, found {}); "
              + "using 3 instead.",
          logPrefix,
          CoreDriverOption.METRICS_NODE_CQL_MESSAGES_DIGITS,
          d);
      significantDigits = 3;
    }
    Duration refreshInterval =
        config.getDuration(CoreDriverOption.METRICS_NODE_CQL_MESSAGES_INTERVAL);

    // Initialize eagerly
    metricRegistry.timer(
        metricNamePrefix + CoreNodeMetric.cql_messages,
        () ->
            new Timer(
                new HdrReservoir(
                    highestLatency,
                    significantDigits,
                    refreshInterval,
                    metricNamePrefix + "." + CoreNodeMetric.cql_messages)));
  }
}
