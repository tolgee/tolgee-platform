package io.tolgee.batch.state

import io.tolgee.component.LockingProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.redisson.api.RAtomicLong
import org.redisson.api.RBatch
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis-based implementation of BatchJobStateProvider.
 * Uses Redisson for distributed state management.
 *
 * ## Performance considerations
 *
 * [ExecutionState] stores only lightweight metadata (status enum, counts, flags) — NOT
 * the full successTargets list. This is critical: previously each hash entry contained
 * a serialized list of potentially thousands of target IDs, making HGETALL / HVALS on
 * large batch jobs take 100–500 ms and block Redis's single thread. With the lightweight
 * state, each entry is ~50 bytes, so even 10,000 entries total ~500 KB — well within
 * acceptable limits for periodic reads.
 *
 * The full successTargets list is persisted to the database only (via BatchJobService)
 * and is never stored in Redis.
 *
 * These maps serialize ExecutionState (incl. its BatchJobChunkExecutionStatus enum) on the RedissonClient default
 * ordinal codec; a codec switch or enum reorder makes pre-deploy values unreadable, with no TTL to recover.
 */
open class RedisBatchJobStateStorage(
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
    private const val REDIS_SINGLE_CHUNK_PROGRESS_COUNT_KEY_PREFIX = "batch_job_single_chunk_progress:"
    private const val REDIS_FAILED_COUNT_KEY_PREFIX = "batch_job_failed:"
    private const val REDIS_CANCELLED_COUNT_KEY_PREFIX = "batch_job_cancelled:"
    private const val REDIS_COMMITTED_COUNT_KEY_PREFIX = "batch_job_committed:"
    private const val REDIS_STARTED_KEY_PREFIX = "batch_job_started:"

    private const val CLEANUP_BATCH_SIZE = 100
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

  override fun getSingleChunkProgressCount(jobId: Long): Long {
    return redissonClient.getAtomicLong("$REDIS_SINGLE_CHUNK_PROGRESS_COUNT_KEY_PREFIX$jobId").get()
  }

  override fun addSingleChunkProgressCount(
    jobId: Long,
    delta: Long,
  ) {
    redissonClient.getAtomicLong("$REDIS_SINGLE_CHUNK_PROGRESS_COUNT_KEY_PREFIX$jobId").addAndGet(delta)
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
    val bucket = redissonClient.getBucket<Boolean>(startedKey(jobId))
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

  override fun removeJobState(jobId: Long) {
    logger.debug("Removing job state for job $jobId")
    val batch = redissonClient.createBatch()
    addCounterDeletesToBatch(batch, jobId)
    batch.getMap<Long, ExecutionState>(stateKey(jobId)).deleteAsync()
    batch.getBucket<Boolean>(initializedKey(jobId)).deleteAsync()
    batch.getBucket<Boolean>(startedKey(jobId)).deleteAsync()
    batch.execute()
    localInitializedJobs.remove(jobId)
  }

  override fun hasCachedJobState(jobId: Long): Boolean {
    return !getRedisHashForJob(jobId).isEmpty()
  }

  override fun getCachedJobIds(): MutableSet<Long> {
    val keys = redissonClient.keys.getKeysByPattern("$REDIS_STATE_KEY_PREFIX*")
    return keys.mapNotNull { it.removePrefix(REDIS_STATE_KEY_PREFIX).toLongOrNull() }.toMutableSet()
  }

  /**
   * Cleans up batch job state hashes where all executions have a completed status.
   */
  override fun clearUnusedStates() {
    getCachedJobIds().chunked(CLEANUP_BATCH_SIZE).forEach { clearCompletedStates(it) }
  }

  private fun clearCompletedStates(jobIds: List<Long>) {
    val completedJobIds =
      readStates(jobIds).mapNotNull { (jobId, values) ->
        val allCompleted = values != null && values.all { it.status.completed }
        jobId.takeIf { allCompleted }
      }
    if (completedJobIds.isEmpty()) {
      return
    }

    // Counters stay live until removeJobState finalizes the job; deleting them here corrupts status updates.
    val deleteBatch = redissonClient.createBatch()
    completedJobIds.forEach { jobId ->
      deleteBatch.getMap<Long, ExecutionState>(stateKey(jobId)).deleteAsync()
      deleteBatch.getBucket<Boolean>(initializedKey(jobId)).deleteAsync()
    }
    deleteBatch.execute()
    completedJobIds.forEach { localInitializedJobs.remove(it) }
  }

  private fun readStates(jobIds: List<Long>): Map<Long, Collection<ExecutionState>?> {
    return try {
      val readBatch = redissonClient.createBatch()
      val futures =
        jobIds.associateWith { jobId ->
          readBatch.getMap<Long, ExecutionState>(stateKey(jobId)).readAllValuesAsync()
        }
      readBatch.execute()
      futures.mapValues { it.value.get() }
    } catch (e: Exception) {
      logger.warn("Pipelined batch-job state read failed; falling back to per-job reads", e)
      jobIds.associateWith { readStateOrNull(it) }
    }
  }

  private fun readStateOrNull(jobId: Long): Collection<ExecutionState>? {
    return try {
      getRedisHashForJob(jobId).readAllValues()
    } catch (e: Exception) {
      logger.warn("Failed to read batch job state for job $jobId during cleanup", e)
      null
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
    return redissonClient.getMap(stateKey(jobId))
  }

  private fun stateKey(jobId: Long) = "$REDIS_STATE_KEY_PREFIX$jobId"

  private fun initializedKey(jobId: Long) = "$REDIS_STATE_INITIALIZED_KEY_PREFIX$jobId"

  private fun startedKey(jobId: Long) = "$REDIS_STARTED_KEY_PREFIX$jobId"

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
    val initKey = initializedKey(jobId)
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

  private fun addCounterDeletesToBatch(
    batch: RBatch,
    jobId: Long,
  ) {
    batch.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").deleteAsync()
    batch.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").deleteAsync()
    batch.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId").deleteAsync()
    batch.getAtomicLong("$REDIS_SINGLE_CHUNK_PROGRESS_COUNT_KEY_PREFIX$jobId").deleteAsync()
    batch.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId").deleteAsync()
    batch.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId").deleteAsync()
    batch.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId").deleteAsync()
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
