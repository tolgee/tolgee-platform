package io.tolgee.batch.state

import io.tolgee.model.batch.BatchJobChunkExecutionStatus

/**
 * Lightweight state stored per chunk execution in Redis (or local memory).
 *
 * Only metadata needed for coordination is kept here — specifically, we store
 * [successTargetsCount] (an Int) instead of the full successTargets list.
 * The full list is persisted to the database and is not needed for any of the
 * Redis-side operations (progress tracking, completion checks, cleanup).
 *
 * Keeping the hash values small is critical for Redis performance because:
 * - HGETALL / HVALS must serialize every value in a single-threaded Redis pass
 * - Large values (e.g. lists of thousands of IDs) block the event loop for
 *   hundreds of milliseconds, causing cascading timeouts on all clients
 */
data class ExecutionState(
  var successTargetsCount: Int,
  var status: BatchJobChunkExecutionStatus,
  var chunkNumber: Int?,
  var retry: Boolean?,
  var transactionCommitted: Boolean,
)
