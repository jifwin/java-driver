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
package com.datastax.oss.driver.api.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.retry.RetryDecision;
import com.datastax.oss.driver.api.core.retry.RetryPolicy;
import com.datastax.oss.driver.api.core.retry.WriteType;
import com.datastax.oss.driver.api.core.servererrors.CoordinatorException;
import com.datastax.oss.driver.api.core.servererrors.ReadTimeoutException;
import com.datastax.oss.driver.api.core.servererrors.UnavailableException;
import com.datastax.oss.driver.api.core.servererrors.WriteTimeoutException;
import com.datastax.oss.driver.api.core.session.Request;

public enum CoreNodeMetric implements NodeMetric {

  /**
   * The number of connections open to this node for regular requests (exposed as a {@link Gauge
   * Gauge&lt;Integer&gt;}.
   *
   * <p>This does not include the control connection (which uses at most one extra connection to a
   * random node in the cluster).
   */
  pooled_connection_count,

  /**
   * The number of <em>stream ids</em> available on the connections to this node (exposed as a
   * {@link Gauge Gauge&lt;Integer&gt;}.
   *
   * <p>Stream ids are used to multiplex requests on each connection, so this is an indication of
   * how many more requests the node could handle concurrently before becoming saturated (note that
   * this is a driver-side only consideration, there might be other limitations on the server that
   * prevent reaching that theoretical limit).
   */
  available_stream_count,

  /**
   * The throughput and latency percentiles of individual CQL messages sent to this node as part of
   * an overall request (exposed as a {@link Timer}).
   *
   * <p>Note that this does not necessarily correspond to the overall duration of the {@link
   * CqlSession#execute(Statement)} call, since the driver might query multiple nodes because of
   * retries and speculative executions. Therefore a single "request" (as seen from a client of the
   * driver) can be composed of more than one of the "messages" measured by this metric.
   *
   * <p>Therefore this metric is intended as an insight into the performance of this particular
   * node. For statistics on overall request completion, use {@link CoreSessionMetric#cql_requests}.
   */
  cql_messages,

  /**
   * The number of times the driver failed to write a request to this node (exposed as a {@link
   * Counter}).
   *
   * <p>In those case we know the request didn't even reach the coordinator, so they are retried on
   * the next node automatically (without going through the retry policy).
   */
  write_errors,

  /**
   * The number of times a request was aborted before the driver even received a response from this
   * node (exposed as a {@link Counter}).
   *
   * <p>See {@link RetryPolicy#onRequestAborted(Request, Throwable, int)} for a description of the
   * cases when this can happen.
   */
  aborted_requests,

  /**
   * The number of times this node replied with a {@code WRITE_TIMEOUT} error (exposed as a {@link
   * Counter}).
   *
   * <p>Whether this error is rethrown directly to the client, rethrown or ignored is determined by
   * the {@link RetryPolicy}.
   *
   * @see WriteTimeoutException
   */
  write_timeouts,

  /**
   * The number of times this node replied with a {@code READ_TIMEOUT} error (exposed as a {@link
   * Counter}).
   *
   * <p>Whether this error is rethrown directly to the client, rethrown or ignored is determined by
   * the {@link RetryPolicy}.
   *
   * @see ReadTimeoutException
   */
  read_timeouts,

  /**
   * The number of times this node replied with an {@code UNAVAILABLE} error (exposed as a {@link
   * Counter}).
   *
   * <p>Whether this error is rethrown directly to the client, rethrown or ignored is determined by
   * the {@link RetryPolicy}.
   *
   * @see UnavailableException
   */
  unavailables,

  /**
   * The number of times this node replied with an error that doesn't fall under {@link
   * #write_timeouts}, {@link #read_timeouts} or {@link #unavailables} (exposed as a {@link
   * Counter}).
   */
  other_errors,

  /**
   * The number of errors on this node that caused the {@link RetryPolicy} to trigger a retry
   * (exposed as a {@link Counter}).
   *
   * <p>This is a sum of all the other {@code retries_on_*} metrics.
   *
   * @see RetryDecision#RETRY_SAME
   * @see RetryDecision#RETRY_NEXT
   */
  retries,

  /**
   * The number of aborted requests on this node that caused the {@link RetryPolicy} to trigger a
   * retry (exposed as a {@link Counter}).
   *
   * @see #aborted_requests
   * @see RetryPolicy#onRequestAborted(Request, Throwable, int)
   */
  retries_on_aborted,

  /**
   * The number of read timeouts on this node that caused the {@link RetryPolicy} to trigger a retry
   * (exposed as a {@link Counter}).
   *
   * @see #read_timeouts
   * @see RetryPolicy#onReadTimeout(Request, ConsistencyLevel, int, int, boolean, int)
   */
  retries_on_read_timeout,

  /**
   * The number of write timeouts on this node that caused the {@link RetryPolicy} to trigger a
   * retry (exposed as a {@link Counter}).
   *
   * @see #write_timeouts
   * @see RetryPolicy#onWriteTimeout(Request, ConsistencyLevel, WriteType, int, int, int)
   */
  retries_on_write_timeout,

  /**
   * The number of unavailable errors on this node that caused the {@link RetryPolicy} to trigger a
   * retry (exposed as a {@link Counter}).
   *
   * @see #unavailables
   * @see RetryPolicy#onUnavailable(Request, ConsistencyLevel, int, int, int)
   */
  retries_on_unavailable,

  /**
   * The number of other errors (neither read or write timeouts, nor unavailables) on this node that
   * caused the {@link RetryPolicy} to trigger a retry (exposed as a {@link Counter}).
   *
   * @see #other_errors
   * @see RetryPolicy#onErrorResponse(Request, CoordinatorException, int)
   */
  retries_on_other_error,

  /**
   * The number of errors on this node that were ignored by the {@link RetryPolicy} (exposed as a
   * {@link Counter}).
   *
   * <p>This is a sum of all the other {@code ignores_on_*} metrics.
   *
   * @see RetryDecision#IGNORE
   */
  ignores,

  /**
   * The number of aborted requests on this node that were ignored by the {@link RetryPolicy}
   * (exposed as a {@link Counter}).
   *
   * @see #aborted_requests
   * @see RetryPolicy#onRequestAborted(Request, Throwable, int)
   */
  ignores_on_aborted,

  /**
   * The number of read timeouts on this node that were ignored by the {@link RetryPolicy} (exposed
   * as a {@link Counter}).
   *
   * @see #read_timeouts
   * @see RetryPolicy#onReadTimeout(Request, ConsistencyLevel, int, int, boolean, int)
   */
  ignores_on_read_timeout,

  /**
   * The number of write timeouts on this node that were ignored by the {@link RetryPolicy} (exposed
   * as a {@link Counter}).
   *
   * @see #write_timeouts
   * @see RetryPolicy#onWriteTimeout(Request, ConsistencyLevel, WriteType, int, int, int)
   */
  ignores_on_write_timeout,

  /**
   * The number of unavailable errors on this node that were ignored by the {@link RetryPolicy}
   * (exposed as a {@link Counter}).
   *
   * @see #unavailables
   * @see RetryPolicy#onUnavailable(Request, ConsistencyLevel, int, int, int)
   */
  ignores_on_unavailable,

  /**
   * The number of other errors (neither read or write timeouts, nor unavailables) on this node that
   * were ignored by the {@link RetryPolicy} (exposed as a {@link Counter}).
   *
   * @see #other_errors
   * @see RetryPolicy#onErrorResponse(Request, CoordinatorException, int)
   */
  ignores_on_other_error,

  /**
   * The number of speculative executions triggered by a slow response from this node (exposed as a
   * {@link Counter}.
   */
  speculative_executions,

  /**
   * The number of errors encountered while trying to establish a connection to this node (exposed
   * as a {@link Counter}).
   *
   * <p>Connection errors are not a fatal issue for the driver, failed connections will be retried
   * periodically according to the reconnection policy. You can choose whether or not to log those
   * errors at {@code WARN} level with the {@code connection.warn-on-init-error} option in the
   * configuration.
   *
   * <p>Authentication errors are not included in this counter, they are tracked in a dedicated
   * metric ({@link #authentication_errors}).
   */
  connection_errors,

  /**
   * The number of authentication errors encountered while trying to establish a connection to this
   * node (exposed as a {@link Counter}).
   *
   * <p>Authentication errors are also logged at {@code WARN} level.
   */
  authentication_errors,
  ;
}
