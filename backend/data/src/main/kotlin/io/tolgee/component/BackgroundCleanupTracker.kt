package io.tolgee.component

/**
 * Implemented by services that perform background (async) data cleanup.
 *
 * [CleanDbTestListener] collects all beans of this type and calls
 * [waitForPendingCleanups] before truncating the database between tests,
 * preventing deadlocks between the cleanup worker's row-level locks and
 * the listener's ALTER TABLE … DISABLE TRIGGER ALL statements.
 */
interface BackgroundCleanupTracker {
  /**
   * Block until all in-flight cleanups started by this service have finished
   * (either successfully or with an error).
   */
  fun waitForPendingCleanups(timeoutMs: Long = 30_000)
}
