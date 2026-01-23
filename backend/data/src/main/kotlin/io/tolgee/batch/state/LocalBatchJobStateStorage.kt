package io.tolgee.batch.state

import io.tolgee.component.LockingProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.util.Logging
import io.tolgee.util.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Local (in-memory) implementation of BatchJobStateProvider.
 * Uses ConcurrentHashMaps with atomic counters for thread-safety.
 */
class LocalBatchJobStateStorage(
  private val initializer: BatchJobStateInitializer,
  private val lockingProvider: LockingProvider,
) : BatchJobStateProvider,
  Logging {
  private val jobStatesMap = ConcurrentHashMap<Long, MutableMap<Long, ExecutionState>>()
  private val runningCountMap = ConcurrentHashMap<Long, AtomicInteger>()
  private val completedChunksCountMap = ConcurrentHashMap<Long, AtomicInteger>()
  private val progressCountMap = ConcurrentHashMap<Long, AtomicLong>()
  private val singleChunkProgressCountMap = ConcurrentHashMap<Long, AtomicLong>()
  private val failedCountMap = ConcurrentHashMap<Long, AtomicInteger>()
  private val cancelledCountMap = ConcurrentHashMap<Long, AtomicInteger>()
  private val committedCountMap = ConcurrentHashMap<Long, AtomicInteger>()
  private val initializedJobs = ConcurrentHashMap.newKeySet<Long>()
  private val startedJobs = ConcurrentHashMap.newKeySet<Long>()

  override fun updateSingleExecution(
    jobId: Long,
    executionId: Long,
    state: ExecutionState,
  ) {
    lockingProvider.withLocking("batch_job_state_lock_$jobId") {
      val map = getLocal(jobId)
      map[executionId] = state
    }
  }

  override fun ensureInitialized(jobId: Long) {
    getLocal(jobId)
  }

  override fun removeSingleExecution(
    jobId: Long,
    executionId: Long,
  ) {
    lockingProvider.withLocking("batch_job_state_lock_$jobId") {
      jobStatesMap[jobId]?.remove(executionId)
    }
  }

  override fun getSingleExecution(
    jobId: Long,
    executionId: Long,
  ): ExecutionState? {
    return jobStatesMap[jobId]?.get(executionId)
  }

  override fun getRunningCount(jobId: Long): Int {
    return runningCountMap[jobId]?.get() ?: 0
  }

  override fun incrementRunningCount(jobId: Long) {
    runningCountMap
      .computeIfAbsent(jobId) { AtomicInteger(0) }
      .incrementAndGet()
  }

  override fun incrementRunningCountAndGet(jobId: Long): Int {
    return runningCountMap
      .computeIfAbsent(jobId) { AtomicInteger(0) }
      .incrementAndGet()
  }

  override fun tryIncrementRunningCount(
    jobId: Long,
    maxConcurrency: Int,
  ): Boolean {
    val counter = runningCountMap.computeIfAbsent(jobId) { AtomicInteger(0) }
    while (true) {
      val current = counter.get()
      if (current >= maxConcurrency) {
        return false
      }
      if (counter.compareAndSet(current, current + 1)) {
        return true
      }
      // CAS failed, retry
    }
  }

  override fun decrementRunningCount(jobId: Long) {
    runningCountMap[jobId]?.decrementAndGet()
  }

  override fun getCompletedChunksCount(jobId: Long): Int {
    return completedChunksCountMap[jobId]?.get() ?: 0
  }

  override fun incrementCompletedChunksCount(jobId: Long) {
    completedChunksCountMap
      .computeIfAbsent(jobId) { AtomicInteger(0) }
      .incrementAndGet()
  }

  override fun incrementCompletedChunksCountAndGet(jobId: Long): Int {
    return completedChunksCountMap
      .computeIfAbsent(jobId) { AtomicInteger(0) }
      .incrementAndGet()
  }

  override fun getProgressCount(jobId: Long): Long {
    return progressCountMap[jobId]?.get() ?: 0L
  }

  override fun addProgressCount(
    jobId: Long,
    delta: Long,
  ) {
    progressCountMap
      .computeIfAbsent(jobId) { AtomicLong(0) }
      .addAndGet(delta)
  }

  override fun getSingleChunkProgressCount(jobId: Long): Long {
    return singleChunkProgressCountMap[jobId]?.get() ?: 0L
  }

  override fun addSingleChunkProgressCount(
    jobId: Long,
    delta: Long,
  ) {
    singleChunkProgressCountMap
      .computeIfAbsent(jobId) { AtomicLong(0) }
      .addAndGet(delta)
  }

  override fun getFailedCount(jobId: Long): Int {
    return failedCountMap[jobId]?.get() ?: 0
  }

  override fun incrementFailedCount(jobId: Long) {
    failedCountMap
      .computeIfAbsent(jobId) { AtomicInteger(0) }
      .incrementAndGet()
  }

  override fun getCancelledCount(jobId: Long): Int {
    return cancelledCountMap[jobId]?.get() ?: 0
  }

  override fun incrementCancelledCount(jobId: Long) {
    cancelledCountMap
      .computeIfAbsent(jobId) { AtomicInteger(0) }
      .incrementAndGet()
  }

  override fun getCommittedCount(jobId: Long): Int {
    return committedCountMap[jobId]?.get() ?: 0
  }

  override fun incrementCommittedCountAndGet(jobId: Long): Int {
    return committedCountMap
      .computeIfAbsent(jobId) { AtomicInteger(0) }
      .incrementAndGet()
  }

  override fun tryMarkJobStarted(jobId: Long): Boolean {
    return startedJobs.add(jobId)
  }

  override fun get(jobId: Long): MutableMap<Long, ExecutionState> {
    return getLocal(jobId)
  }

  override fun getCached(jobId: Long): MutableMap<Long, ExecutionState>? {
    return jobStatesMap[jobId]
  }

  override fun removeJobState(jobId: Long): MutableMap<Long, ExecutionState>? {
    logger.debug("Removing job state for job $jobId")
    removeAllCounters(jobId)
    initializedJobs.remove(jobId)
    startedJobs.remove(jobId)
    return jobStatesMap.remove(jobId)
  }

  override fun hasCachedJobState(jobId: Long): Boolean {
    return jobStatesMap.containsKey(jobId)
  }

  override fun getCachedJobIds(): MutableSet<Long> {
    return jobStatesMap.keys.toMutableSet()
  }

  override fun clearUnusedStates() {
    val toRemove =
      jobStatesMap
        .filter { it.value.all { (_, state) -> state.status.completed } }
        .keys
    toRemove.forEach { jobId ->
      jobStatesMap.remove(jobId)
      initializedJobs.remove(jobId)
      // Do NOT remove counters here - they're needed until job status is properly updated
      // Counters will be removed in removeJobState when job is finalized
    }
  }

  override fun clearAllState() {
    jobStatesMap.clear()
    runningCountMap.clear()
    completedChunksCountMap.clear()
    progressCountMap.clear()
    singleChunkProgressCountMap.clear()
    failedCountMap.clear()
    cancelledCountMap.clear()
    committedCountMap.clear()
    initializedJobs.clear()
    startedJobs.clear()
  }

  override fun getStateForExecution(execution: BatchJobChunkExecution): ExecutionState {
    return initializer.getStateForExecution(execution)
  }

  override fun getInitialState(jobId: Long): MutableMap<Long, ExecutionState> {
    return initializer.getInitialState(jobId)
  }

  private fun getLocal(jobId: Long): MutableMap<Long, ExecutionState> {
    val existing = jobStatesMap[jobId]
    if (existing != null) {
      return existing
    }
    // Need to initialize - use synchronized to prevent double initialization
    synchronized(initializedJobs) {
      // Double-check after acquiring lock
      jobStatesMap[jobId]?.let { return it }
      val initialState = initializer.getInitialState(jobId)
      jobStatesMap[jobId] = initialState
      initializeCountersFromState(jobId, initialState)
      initializedJobs.add(jobId)
      return initialState
    }
  }

  private fun removeAllCounters(jobId: Long) {
    runningCountMap.remove(jobId)
    completedChunksCountMap.remove(jobId)
    progressCountMap.remove(jobId)
    singleChunkProgressCountMap.remove(jobId)
    failedCountMap.remove(jobId)
    cancelledCountMap.remove(jobId)
    committedCountMap.remove(jobId)
  }

  private fun initializeCountersFromState(
    jobId: Long,
    state: Map<Long, ExecutionState>,
  ) {
    val counters = initializer.calculateCountersFromState(state)
    setLocalCounterIfLower(runningCountMap, jobId, counters.runningCount)
    setLocalCounterIfLower(completedChunksCountMap, jobId, counters.completedChunksCount)
    setLocalLongCounterIfLower(progressCountMap, jobId, counters.progressCount)
    setLocalCounterIfLower(failedCountMap, jobId, counters.failedCount)
    setLocalCounterIfLower(cancelledCountMap, jobId, counters.cancelledCount)
    setLocalCounterIfLower(committedCountMap, jobId, counters.committedCount)
  }

  private fun setLocalCounterIfLower(
    map: ConcurrentHashMap<Long, AtomicInteger>,
    jobId: Long,
    value: Int,
  ) {
    map.compute(jobId) { _, existing ->
      if (existing == null) {
        AtomicInteger(value)
      } else {
        // Only update if existing value is lower than the initial value
        while (true) {
          val current = existing.get()
          if (current >= value) {
            break
          }
          if (existing.compareAndSet(current, value)) {
            break
          }
        }
        existing
      }
    }
  }

  private fun setLocalLongCounterIfLower(
    map: ConcurrentHashMap<Long, AtomicLong>,
    jobId: Long,
    value: Long,
  ) {
    map.compute(jobId) { _, existing ->
      if (existing == null) {
        AtomicLong(value)
      } else {
        // Only update if existing value is lower than the initial value
        while (true) {
          val current = existing.get()
          if (current >= value) {
            break
          }
          if (existing.compareAndSet(current, value)) {
            break
          }
        }
        existing
      }
    }
  }
}
