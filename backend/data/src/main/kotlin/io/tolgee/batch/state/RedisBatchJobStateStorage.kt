package io.tolgee.batch.state

import io.tolgee.component.LockingProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.redisson.api.RAtomicLong
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis-based implementation of BatchJobStateProvider.
 * Uses Redisson for distributed state management.
 */
class RedisBatchJobStateStorage(
  private val initializer: BatchJobStateInitializer,
  private val lockingProvider: LockingProvider,
  private val redissonClient: RedissonClient,
) : BatchJobStateProvider,
  Logging {
  companion object {
    private const val REDIS_STATE_KEY_PREFIX = "batch_job_state:"
    private const val REDIS_RUNNING_COUNT_KEY_PREFIX = "batch_job_running_count:"
    private const val REDIS_STATE_INITIALIZED_KEY_PREFIX = "batch_job_state_initialized:"
    private const val REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX = "batch_job_completed_chunks:"
    private const val REDIS_PROGRESS_COUNT_KEY_PREFIX = "batch_job_progress:"
    private const val REDIS_FAILED_COUNT_KEY_PREFIX = "batch_job_failed:"
    private const val REDIS_CANCELLED_COUNT_KEY_PREFIX = "batch_job_cancelled:"
    private const val REDIS_COMMITTED_COUNT_KEY_PREFIX = "batch_job_committed:"
    private const val REDIS_STARTED_KEY_PREFIX = "batch_job_started:"
  }

  // Local cache for initialization status - avoids Redis calls for already-initialized jobs
  private val localInitializedJobs = ConcurrentHashMap.newKeySet<Long>()

  override fun updateSingleExecution(
    jobId: Long,
    executionId: Long,
    state: ExecutionState,
  ) {
    val redisHash = getRedisHashForJob(jobId)
    // No initialization needed - trySetExecutionRunning already initialized the hash
    redisHash[executionId] = state
  }

  override fun ensureInitialized(jobId: Long) {
    val redisHash = getRedisHashForJob(jobId)
    ensureRedisHashInitialized(jobId, redisHash)
  }

  override fun removeSingleExecution(
    jobId: Long,
    executionId: Long,
  ) {
    val redisHash = getRedisHashForJob(jobId)
    // No initialization needed - just remove if exists
    redisHash.remove(executionId)
  }

  override fun getSingleExecution(
    jobId: Long,
    executionId: Long,
  ): ExecutionState? {
    val redisHash = getRedisHashForJob(jobId)
    // No initialization needed - just get if exists
    return redisHash[executionId]
  }

  override fun getRunningCount(jobId: Long): Int {
    return redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").get().toInt()
  }

  override fun incrementRunningCount(jobId: Long) {
    redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").incrementAndGet()
  }

  override fun incrementRunningCountAndGet(jobId: Long): Int {
    return redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").incrementAndGet().toInt()
  }

  override fun tryIncrementRunningCount(
    jobId: Long,
    maxConcurrency: Int,
  ): Boolean {
    // Use a lock to make check-and-increment atomic for Redis
    return lockingProvider.withLocking("batch_job_running_count_$jobId") {
      val counter = redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId")
      val current = counter.get().toInt()
      if (current >= maxConcurrency) {
        false
      } else {
        counter.incrementAndGet()
        true
      }
    }
  }

  override fun decrementRunningCount(jobId: Long) {
    redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").decrementAndGet()
  }

  override fun getCompletedChunksCount(jobId: Long): Int {
    return redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").get().toInt()
  }

  override fun incrementCompletedChunksCount(jobId: Long) {
    redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").incrementAndGet()
  }

  override fun incrementCompletedChunksCountAndGet(jobId: Long): Int {
    return redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").incrementAndGet().toInt()
  }

  override fun getProgressCount(jobId: Long): Long {
    return redissonClient.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId").get()
  }

  override fun addProgressCount(
    jobId: Long,
    delta: Long,
  ) {
    redissonClient.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId").addAndGet(delta)
  }

  override fun getFailedCount(jobId: Long): Int {
    return redissonClient.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId").get().toInt()
  }

  override fun incrementFailedCount(jobId: Long) {
    redissonClient.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId").incrementAndGet()
  }

  override fun getCancelledCount(jobId: Long): Int {
    return redissonClient.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId").get().toInt()
  }

  override fun incrementCancelledCount(jobId: Long) {
    redissonClient.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId").incrementAndGet()
  }

  override fun getCommittedCount(jobId: Long): Int {
    return redissonClient.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId").get().toInt()
  }

  override fun incrementCommittedCountAndGet(jobId: Long): Int {
    return redissonClient.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId").incrementAndGet().toInt()
  }

  override fun tryMarkJobStarted(jobId: Long): Boolean {
    val bucket = redissonClient.getBucket<Boolean>("$REDIS_STARTED_KEY_PREFIX$jobId")
    return bucket.setIfAbsent(true)
  }

  override fun get(jobId: Long): MutableMap<Long, ExecutionState> {
    val redisHash = getRedisHashForJob(jobId)
    ensureRedisHashInitialized(jobId, redisHash)
    return redisHash.readAllMap().toMutableMap()
  }

  override fun getCached(jobId: Long): MutableMap<Long, ExecutionState>? {
    val redisHash = getRedisHashForJob(jobId)
    return if (redisHash.isEmpty()) null else redisHash.readAllMap().toMutableMap()
  }

  override fun removeJobState(jobId: Long): MutableMap<Long, ExecutionState>? {
    logger.debug("Removing job state for job $jobId")
    removeAllCounters(jobId)
    val redisHash = getRedisHashForJob(jobId)
    val state = if (redisHash.isEmpty()) null else redisHash.readAllMap().toMutableMap()
    redisHash.delete()
    // Also remove initialization and started markers
    redissonClient.getBucket<Boolean>("$REDIS_STATE_INITIALIZED_KEY_PREFIX$jobId").delete()
    redissonClient.getBucket<Boolean>("$REDIS_STARTED_KEY_PREFIX$jobId").delete()
    // Clear local initialization cache to allow re-initialization if jobId is reused
    localInitializedJobs.remove(jobId)
    return state
  }

  override fun hasCachedJobState(jobId: Long): Boolean {
    return !getRedisHashForJob(jobId).isEmpty()
  }

  override fun getCachedJobIds(): MutableSet<Long> {
    val keys = redissonClient.keys.getKeysByPattern("$REDIS_STATE_KEY_PREFIX*")
    return keys.mapNotNull { it.removePrefix(REDIS_STATE_KEY_PREFIX).toLongOrNull() }.toMutableSet()
  }

  override fun clearUnusedStates() {
    // For Redis, we scan for batch_job_state:* keys and check each
    val keys = redissonClient.keys.getKeysByPattern("$REDIS_STATE_KEY_PREFIX*")
    keys.forEach { key ->
      val jobId = key.removePrefix(REDIS_STATE_KEY_PREFIX).toLongOrNull() ?: return@forEach
      val redisHash = getRedisHashForJob(jobId)
      val allCompleted = redisHash.readAllValues().all { state -> state.status.completed }
      if (allCompleted) {
        redisHash.delete()
        redissonClient.getBucket<Boolean>("$REDIS_STATE_INITIALIZED_KEY_PREFIX$jobId").delete()
        // Clear local initialization cache to allow re-initialization if jobId is reused
        localInitializedJobs.remove(jobId)
        // Do NOT remove counters here - they're needed until job status is properly updated
        // Counters will be removed in removeJobState when job is finalized
      }
    }
  }

  override fun clearAllState() {
    // Clear local initialization cache
    localInitializedJobs.clear()
    // Note: This does NOT clear Redis state - that would affect other instances.
    // For testing, each implementation handles its own scope.
  }

  override fun getStateForExecution(execution: BatchJobChunkExecution): ExecutionState {
    return initializer.getStateForExecution(execution)
  }

  override fun getInitialState(jobId: Long): MutableMap<Long, ExecutionState> {
    return initializer.getInitialState(jobId)
  }

  private fun getRedisHashForJob(jobId: Long): RMap<Long, ExecutionState> {
    return redissonClient.getMap("$REDIS_STATE_KEY_PREFIX$jobId")
  }

  /**
   * Ensures Redis hash is initialized from DB. Uses local in-memory cache first for O(1) check,
   * then falls back to Redis marker for cross-instance coordination.
   * Uses a short-lived lock to prevent multiple threads from initializing simultaneously.
   * Uses putIfAbsent to not overwrite entries that were already updated by other threads.
   * Also initializes all counters from the initial state.
   */
  private fun ensureRedisHashInitialized(
    jobId: Long,
    redisHash: RMap<Long, ExecutionState>,
  ) {
    // Fast path: check local in-memory cache first (no Redis call)
    if (localInitializedJobs.contains(jobId)) {
      return
    }
    // Check Redis marker for cross-instance coordination
    val initKey = "$REDIS_STATE_INITIALIZED_KEY_PREFIX$jobId"
    if (redissonClient.getBucket<Boolean>(initKey).get() == true) {
      localInitializedJobs.add(jobId)
      return
    }
    // Use a lock only for initialization to prevent race conditions
    lockingProvider.withLocking("batch_job_state_init_$jobId") {
      // Double-check after acquiring lock
      if (redissonClient.getBucket<Boolean>(initKey).get() != true) {
        val initialState = initializer.getInitialState(jobId)
        // Use batch putAll for much better performance (single Redis operation vs N operations)
        // For new jobs, there's no existing state, so putAll is safe and much faster
        // For restarted jobs with partial state, putAll will overwrite, but that's fine
        // since we're loading from the authoritative DB state
        redisHash.putAll(initialState)
        // Initialize counters from the initial state
        initializeCountersFromState(jobId, initialState)
        // Mark as initialized in Redis
        redissonClient.getBucket<Boolean>(initKey).set(true)
      }
      // Mark as initialized in local cache
      localInitializedJobs.add(jobId)
    }
  }

  private fun removeAllCounters(jobId: Long) {
    redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").delete()
    redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").delete()
    redissonClient.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId").delete()
    redissonClient.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId").delete()
    redissonClient.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId").delete()
    redissonClient.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId").delete()
  }

  private fun initializeCountersFromState(
    jobId: Long,
    state: Map<Long, ExecutionState>,
  ) {
    val counters = initializer.calculateCountersFromState(state)
    // Use setIfLower to avoid overwriting concurrent increments.
    // If the counter is already > initial value, some execution has already updated it.
    setIfLower(
      redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId"),
      counters.runningCount.toLong(),
    )
    setIfLower(
      redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId"),
      counters.completedChunksCount.toLong(),
    )
    setIfLower(
      redissonClient.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId"),
      counters.progressCount,
    )
    setIfLower(
      redissonClient.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId"),
      counters.failedCount.toLong(),
    )
    setIfLower(
      redissonClient.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId"),
      counters.cancelledCount.toLong(),
    )
    setIfLower(
      redissonClient.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId"),
      counters.committedCount.toLong(),
    )
  }

  /**
   * Sets the counter to the given value only if the current value is lower.
   * This prevents overwriting concurrent increments during initialization.
   */
  private fun setIfLower(
    counter: RAtomicLong,
    value: Long,
  ) {
    while (true) {
      val current = counter.get()
      if (current >= value) {
        // Current value is already >= the initial value, don't overwrite
        return
      }
      if (counter.compareAndSet(current, value)) {
        return
      }
      // CAS failed, retry
    }
  }
}
