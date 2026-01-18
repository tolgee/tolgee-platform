package io.tolgee.batch.state

import io.tolgee.component.LockingProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import java.util.concurrent.ConcurrentHashMap

@Component
class BatchJobStateProvider(
  val usingRedisProvider: UsingRedisProvider,
  @Lazy
  val redissonClient: RedissonClient,
  val entityManager: EntityManager,
  val lockingProvider: LockingProvider,
  val platformTransactionManager: PlatformTransactionManager,
) : Logging {
  companion object {
    private val localJobStatesMap by lazy {
      ConcurrentHashMap<Long, MutableMap<Long, ExecutionState>>()
    }
    private val localRunningCountMap by lazy {
      ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicInteger>()
    }
    private val localCompletedChunksCountMap by lazy {
      ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicInteger>()
    }
    private val localProgressCountMap by lazy {
      ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicLong>()
    }
    private val localFailedCountMap by lazy {
      ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicInteger>()
    }
    private val localCancelledCountMap by lazy {
      ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicInteger>()
    }
    private val localCommittedCountMap by lazy {
      ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicInteger>()
    }

    private const val REDIS_STATE_KEY_PREFIX = "batch_job_state:"
    private const val REDIS_RUNNING_COUNT_KEY_PREFIX = "batch_job_running_count:"
    private const val REDIS_STATE_INITIALIZED_KEY_PREFIX = "batch_job_state_initialized:"
    private const val REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX = "batch_job_completed_chunks:"
    private const val REDIS_PROGRESS_COUNT_KEY_PREFIX = "batch_job_progress:"
    private const val REDIS_FAILED_COUNT_KEY_PREFIX = "batch_job_failed:"
    private const val REDIS_CANCELLED_COUNT_KEY_PREFIX = "batch_job_cancelled:"
    private const val REDIS_COMMITTED_COUNT_KEY_PREFIX = "batch_job_committed:"

    private val localInitializedJobs by lazy {
      ConcurrentHashMap.newKeySet<Long>()
    }
  }

  /**
   * Updates a single execution state without job-level locking.
   * Uses Redis HSET which is atomic per field.
   * Use this when you only need to update one execution and don't need to check other executions atomically.
   * Note: Assumes initialization was already done by trySetExecutionRunning (which uses updateState).
   */
  fun updateSingleExecution(
    jobId: Long,
    executionId: Long,
    state: ExecutionState,
  ) {
    if (usingRedisProvider.areWeUsingRedis) {
      val redisHash = getRedisHashForJob(jobId)
      // No initialization needed - trySetExecutionRunning already initialized the hash
      redisHash[executionId] = state
    } else {
      // For local mode, still need lock to ensure thread-safety within JVM
      lockingProvider.withLocking("batch_job_state_lock_$jobId") {
        val map = getLocal(jobId)
        map[executionId] = state
      }
    }
  }

  /**
   * Ensures the job state is initialized from DB. O(1) check using initialization marker.
   * Call this before using lock-free single-execution operations.
   */
  fun ensureInitialized(jobId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      val redisHash = getRedisHashForJob(jobId)
      ensureRedisHashInitialized(jobId, redisHash)
    } else {
      // For local, getLocal already handles initialization via getOrPut
      getLocal(jobId)
    }
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
        val initialState = getInitialState(jobId)
        // Use putIfAbsent to not overwrite entries updated by other threads
        initialState.forEach { (executionId, state) ->
          redisHash.putIfAbsent(executionId, state)
        }
        // Initialize counters from the initial state
        initializeCountersFromState(jobId, initialState)
        // Mark as initialized in Redis
        redissonClient.getBucket<Boolean>(initKey).set(true)
      }
      // Mark as initialized in local cache
      localInitializedJobs.add(jobId)
    }
  }

  /**
   * Initializes all counters from the given state map.
   * Called during initialization to ensure counters match the DB state.
   */
  private fun initializeCountersFromState(
    jobId: Long,
    state: Map<Long, ExecutionState>,
  ) {
    var runningCount = 0
    var completedChunksCount = 0
    var progressCount = 0L
    var failedCount = 0
    var cancelledCount = 0
    var committedCount = 0

    state.values.forEach { executionState ->
      if (executionState.status == io.tolgee.model.batch.BatchJobChunkExecutionStatus.RUNNING) {
        runningCount++
      }
      if (executionState.status.completed && executionState.retry != true) {
        completedChunksCount++
      }
      progressCount += executionState.successTargets.size
      if (executionState.status == io.tolgee.model.batch.BatchJobChunkExecutionStatus.FAILED &&
        executionState.retry != true
      ) {
        failedCount++
      }
      if (executionState.status == io.tolgee.model.batch.BatchJobChunkExecutionStatus.CANCELLED) {
        cancelledCount++
      }
      if (executionState.transactionCommitted) {
        committedCount++
      }
    }

    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").set(runningCount.toLong())
      redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").set(completedChunksCount.toLong())
      redissonClient.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId").set(progressCount)
      redissonClient.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId").set(failedCount.toLong())
      redissonClient.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId").set(cancelledCount.toLong())
      redissonClient.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId").set(committedCount.toLong())
    } else {
      localRunningCountMap[jobId] = java.util.concurrent.atomic.AtomicInteger(runningCount)
      localCompletedChunksCountMap[jobId] = java.util.concurrent.atomic.AtomicInteger(completedChunksCount)
      localProgressCountMap[jobId] = java.util.concurrent.atomic.AtomicLong(progressCount)
      localFailedCountMap[jobId] = java.util.concurrent.atomic.AtomicInteger(failedCount)
      localCancelledCountMap[jobId] = java.util.concurrent.atomic.AtomicInteger(cancelledCount)
      localCommittedCountMap[jobId] = java.util.concurrent.atomic.AtomicInteger(committedCount)
    }
  }

  /**
   * Removes a single execution state without job-level locking.
   * Uses Redis HDEL which is atomic per field.
   */
  fun removeSingleExecution(
    jobId: Long,
    executionId: Long,
  ) {
    if (usingRedisProvider.areWeUsingRedis) {
      val redisHash = getRedisHashForJob(jobId)
      // No initialization needed - just remove if exists
      redisHash.remove(executionId)
    } else {
      lockingProvider.withLocking("batch_job_state_lock_$jobId") {
        val map = localJobStatesMap[jobId]
        map?.remove(executionId)
      }
    }
  }

  /**
   * Gets a single execution state without job-level locking.
   * Uses Redis HGET which is atomic per field.
   */
  fun getSingleExecution(
    jobId: Long,
    executionId: Long,
  ): ExecutionState? {
    if (usingRedisProvider.areWeUsingRedis) {
      val redisHash = getRedisHashForJob(jobId)
      // No initialization needed - just get if exists
      return redisHash[executionId]
    }
    return localJobStatesMap[jobId]?.get(executionId)
  }

  /**
   * Gets the running count for a job. O(1) operation using atomic counter.
   */
  fun getRunningCount(jobId: Long): Int {
    if (usingRedisProvider.areWeUsingRedis) {
      return redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").get().toInt()
    }
    return localRunningCountMap[jobId]?.get() ?: 0
  }

  /**
   * Increments the running count for a job. O(1) atomic operation.
   */
  fun incrementRunningCount(jobId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").incrementAndGet()
    } else {
      localRunningCountMap
        .computeIfAbsent(jobId) {
          java.util.concurrent.atomic
            .AtomicInteger(0)
        }.incrementAndGet()
    }
  }

  /**
   * Decrements the running count for a job. O(1) atomic operation.
   */
  fun decrementRunningCount(jobId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").decrementAndGet()
    } else {
      localRunningCountMap[jobId]?.decrementAndGet()
    }
  }

  // ===== Completed Chunks Counter =====

  fun getCompletedChunksCount(jobId: Long): Int {
    if (usingRedisProvider.areWeUsingRedis) {
      return redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").get().toInt()
    }
    return localCompletedChunksCountMap[jobId]?.get() ?: 0
  }

  fun incrementCompletedChunksCount(jobId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").incrementAndGet()
    } else {
      localCompletedChunksCountMap
        .computeIfAbsent(jobId) {
          java.util.concurrent.atomic
            .AtomicInteger(0)
        }.incrementAndGet()
    }
  }

  // ===== Progress Counter =====

  fun getProgressCount(jobId: Long): Long {
    if (usingRedisProvider.areWeUsingRedis) {
      return redissonClient.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId").get()
    }
    return localProgressCountMap[jobId]?.get() ?: 0L
  }

  fun addProgressCount(
    jobId: Long,
    delta: Long,
  ) {
    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId").addAndGet(delta)
    } else {
      localProgressCountMap
        .computeIfAbsent(jobId) {
          java.util.concurrent.atomic
            .AtomicLong(0)
        }.addAndGet(delta)
    }
  }

  // ===== Failed Counter =====

  fun getFailedCount(jobId: Long): Int {
    if (usingRedisProvider.areWeUsingRedis) {
      return redissonClient.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId").get().toInt()
    }
    return localFailedCountMap[jobId]?.get() ?: 0
  }

  fun incrementFailedCount(jobId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId").incrementAndGet()
    } else {
      localFailedCountMap
        .computeIfAbsent(jobId) {
          java.util.concurrent.atomic
            .AtomicInteger(0)
        }.incrementAndGet()
    }
  }

  // ===== Cancelled Counter =====

  fun getCancelledCount(jobId: Long): Int {
    if (usingRedisProvider.areWeUsingRedis) {
      return redissonClient.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId").get().toInt()
    }
    return localCancelledCountMap[jobId]?.get() ?: 0
  }

  fun incrementCancelledCount(jobId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId").incrementAndGet()
    } else {
      localCancelledCountMap
        .computeIfAbsent(jobId) {
          java.util.concurrent.atomic
            .AtomicInteger(0)
        }.incrementAndGet()
    }
  }

  // ===== Committed Counter =====

  fun getCommittedCount(jobId: Long): Int {
    if (usingRedisProvider.areWeUsingRedis) {
      return redissonClient.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId").get().toInt()
    }
    return localCommittedCountMap[jobId]?.get() ?: 0
  }

  fun incrementCommittedCount(jobId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId").incrementAndGet()
    } else {
      localCommittedCountMap
        .computeIfAbsent(jobId) {
          java.util.concurrent.atomic
            .AtomicInteger(0)
        }.incrementAndGet()
    }
  }

  // ===== Cleanup all counters =====

  private fun removeAllCounters(jobId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      redissonClient.getAtomicLong("$REDIS_RUNNING_COUNT_KEY_PREFIX$jobId").delete()
      redissonClient.getAtomicLong("$REDIS_COMPLETED_CHUNKS_COUNT_KEY_PREFIX$jobId").delete()
      redissonClient.getAtomicLong("$REDIS_PROGRESS_COUNT_KEY_PREFIX$jobId").delete()
      redissonClient.getAtomicLong("$REDIS_FAILED_COUNT_KEY_PREFIX$jobId").delete()
      redissonClient.getAtomicLong("$REDIS_CANCELLED_COUNT_KEY_PREFIX$jobId").delete()
      redissonClient.getAtomicLong("$REDIS_COMMITTED_COUNT_KEY_PREFIX$jobId").delete()
    } else {
      localRunningCountMap.remove(jobId)
      localCompletedChunksCountMap.remove(jobId)
      localProgressCountMap.remove(jobId)
      localFailedCountMap.remove(jobId)
      localCancelledCountMap.remove(jobId)
      localCommittedCountMap.remove(jobId)
    }
  }

  /**
   * Optimized Redis implementation using Redis Hash.
   * Each job has its own hash: batch_job_state:{jobId}
   * Fields are execution IDs, values are ExecutionState objects.
   * Uses distributed lock per job to ensure atomicity of read-modify-write.
   * Only writes back entries that were actually modified to avoid overwriting
   * concurrent lock-free updates.
   */
  private fun <T> updateStateRedis(
    jobId: Long,
    block: (MutableMap<Long, ExecutionState>) -> T,
  ): T {
    return lockingProvider.withLocking("batch_job_state_lock_$jobId") {
      val redisHash = getRedisHashForJob(jobId)
      ensureRedisHashInitialized(jobId, redisHash)

      // Read current state from Redis Hash
      val currentState: MutableMap<Long, ExecutionState> = redisHash.readAllMap().toMutableMap()

      // Snapshot original keys and values for comparison
      val originalKeys = currentState.keys.toSet()
      val originalValues = currentState.mapValues { it.value.copy() }

      // Execute the block
      val result = block(currentState)

      // Only write back entries that were actually modified or added
      currentState.forEach { (executionId, state) ->
        val original = originalValues[executionId]
        if (original == null || original != state) {
          redisHash[executionId] = state
        }
      }

      // Handle removed entries
      originalKeys.forEach { executionId ->
        if (!currentState.containsKey(executionId)) {
          redisHash.remove(executionId)
        }
      }

      result
    }
  }

  /**
   * Local implementation with simple locking.
   */
  private fun <T> updateStateLocal(
    jobId: Long,
    block: (MutableMap<Long, ExecutionState>) -> T,
  ): T {
    return lockingProvider.withLocking("batch_job_state_lock_$jobId") {
      val map = getLocal(jobId)
      val result = block(map)
      localJobStatesMap[jobId] = map
      result
    }
  }

  private fun getRedisHashForJob(jobId: Long): RMap<Long, ExecutionState> {
    return redissonClient.getMap("$REDIS_STATE_KEY_PREFIX$jobId")
  }

  fun getStateForExecution(execution: BatchJobChunkExecution): ExecutionState {
    return ExecutionState(
      successTargets = execution.successTargets,
      status = execution.status,
      chunkNumber = execution.chunkNumber,
      retry = execution.retry,
      transactionCommitted = false,
    )
  }

  /**
   * Doesn't init map if not exists
   */
  fun getCached(jobId: Long): MutableMap<Long, ExecutionState>? {
    if (usingRedisProvider.areWeUsingRedis) {
      val redisHash = getRedisHashForJob(jobId)
      return if (redisHash.isEmpty()) null else redisHash.readAllMap().toMutableMap()
    }
    return localJobStatesMap[jobId]
  }

  fun removeJobState(jobId: Long): MutableMap<Long, ExecutionState>? {
    logger.debug("Removing job state for job $jobId")
    removeAllCounters(jobId)
    if (usingRedisProvider.areWeUsingRedis) {
      val redisHash = getRedisHashForJob(jobId)
      val state = if (redisHash.isEmpty()) null else redisHash.readAllMap().toMutableMap()
      redisHash.delete()
      // Also remove initialization marker
      redissonClient.getBucket<Boolean>("$REDIS_STATE_INITIALIZED_KEY_PREFIX$jobId").delete()
      return state
    }
    localInitializedJobs.remove(jobId)
    return localJobStatesMap.remove(jobId)
  }

  fun hasCachedJobState(jobId: Long): Boolean {
    if (usingRedisProvider.areWeUsingRedis) {
      return !getRedisHashForJob(jobId).isEmpty()
    }
    return localJobStatesMap.containsKey(jobId)
  }

  private fun getLocal(jobId: Long): MutableMap<Long, ExecutionState> {
    val existing = localJobStatesMap[jobId]
    if (existing != null) {
      return existing
    }
    // Need to initialize - use synchronized to prevent double initialization
    synchronized(localInitializedJobs) {
      // Double-check after acquiring lock
      localJobStatesMap[jobId]?.let { return it }
      val initialState = getInitialState(jobId)
      localJobStatesMap[jobId] = initialState
      initializeCountersFromState(jobId, initialState)
      localInitializedJobs.add(jobId)
      return initialState
    }
  }

  fun get(jobId: Long): MutableMap<Long, ExecutionState> {
    if (usingRedisProvider.areWeUsingRedis) {
      val redisHash = getRedisHashForJob(jobId)
      ensureRedisHashInitialized(jobId, redisHash)
      return redisHash.readAllMap().toMutableMap()
    }
    return getLocal(jobId)
  }

  fun getInitialState(jobId: Long): MutableMap<Long, ExecutionState> {
    logger.debug("Initializing batch job state for job $jobId")
    // we want to get state which is not affected by current transaction
    val executions =
      executeInNewTransaction(
        platformTransactionManager,
        isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED,
        readOnly = true,
      ) {
        entityManager
          .createQuery(
            """
      from BatchJobChunkExecution bjce
      where bjce.batchJob.id = :jobId
      """,
            BatchJobChunkExecution::class.java,
          ).setParameter("jobId", jobId)
          .resultList
      }

    return executions
      .associate {
        it.id to
          ExecutionState(
            it.successTargets,
            it.status,
            it.chunkNumber,
            it.retry,
            // Only mark as transactionCommitted if the execution is actually completed
            // PENDING/RUNNING executions haven't committed yet
            it.status.completed,
          )
      }.toMutableMap()
  }

  /**
   * If the state of all execution completed, it's highly probable it is not needed anymore, so we can clean it up.
   * NOTE: This method does NOT remove counters - counters are only removed in removeJobState when the job
   * is properly finalized. This prevents race conditions where counters are removed before the job status
   * is updated based on those counters.
   */
  @Scheduled(fixedRate = 10000)
  fun clearUnusedStates() {
    if (usingRedisProvider.areWeUsingRedis) {
      // For Redis, we scan for batch_job_state:* keys and check each
      val keys = redissonClient.keys.getKeysByPattern("$REDIS_STATE_KEY_PREFIX*")
      keys.forEach { key ->
        val jobId = key.removePrefix(REDIS_STATE_KEY_PREFIX).toLongOrNull() ?: return@forEach
        val redisHash = getRedisHashForJob(jobId)
        val allCompleted = redisHash.readAllValues().all { state -> state.status.completed }
        if (allCompleted) {
          redisHash.delete()
          redissonClient.getBucket<Boolean>("$REDIS_STATE_INITIALIZED_KEY_PREFIX$jobId").delete()
          // Do NOT remove counters here - they're needed until job status is properly updated
          // Counters will be removed in removeJobState when job is finalized
        }
      }
    } else {
      val toRemove =
        localJobStatesMap
          .filter {
            it.value.all { (_, state) -> state.status.completed }
          }.keys
      toRemove.forEach { jobId ->
        localJobStatesMap.remove(jobId)
        localInitializedJobs.remove(jobId)
        // Do NOT remove counters here - they're needed until job status is properly updated
        // Counters will be removed in removeJobState when job is finalized
      }
    }
  }

  fun getCachedJobIds(): MutableSet<Long> {
    if (usingRedisProvider.areWeUsingRedis) {
      val keys = redissonClient.keys.getKeysByPattern("$REDIS_STATE_KEY_PREFIX*")
      return keys.mapNotNull { it.removePrefix(REDIS_STATE_KEY_PREFIX).toLongOrNull() }.toMutableSet()
    }
    return localJobStatesMap.keys.toMutableSet()
  }
}
