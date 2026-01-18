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

    private const val REDIS_STATE_KEY_PREFIX = "batch_job_state:"
  }

  /**
   * Updates the state for a job with distributed locking.
   * Use this when you need atomic read-check-write across multiple entries
   * (e.g., checking running count before starting a new execution).
   */
  fun <T> updateState(
    jobId: Long,
    block: (MutableMap<Long, ExecutionState>) -> T,
  ): T {
    if (usingRedisProvider.areWeUsingRedis) {
      return updateStateRedis(jobId, block)
    }
    return updateStateLocal(jobId, block)
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
   * Ensures Redis hash is initialized from DB. Uses a short-lived lock to prevent
   * multiple threads from initializing simultaneously.
   * Uses putIfAbsent to not overwrite entries that were already updated by other threads.
   */
  private fun ensureRedisHashInitialized(
    jobId: Long,
    redisHash: RMap<Long, ExecutionState>,
  ) {
    if (!redisHash.isEmpty()) {
      return
    }
    // Use a lock only for initialization to prevent race conditions
    lockingProvider.withLocking("batch_job_state_init_$jobId") {
      // Double-check after acquiring lock
      if (redisHash.isEmpty()) {
        val initialState = getInitialState(jobId)
        // Use putIfAbsent to not overwrite entries updated by other threads
        initialState.forEach { (executionId, state) ->
          redisHash.putIfAbsent(executionId, state)
        }
      }
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
    if (usingRedisProvider.areWeUsingRedis) {
      val redisHash = getRedisHashForJob(jobId)
      val state = if (redisHash.isEmpty()) null else redisHash.readAllMap().toMutableMap()
      redisHash.delete()
      return state
    }
    return localJobStatesMap.remove(jobId)
  }

  fun hasCachedJobState(jobId: Long): Boolean {
    if (usingRedisProvider.areWeUsingRedis) {
      return !getRedisHashForJob(jobId).isEmpty()
    }
    return localJobStatesMap.containsKey(jobId)
  }

  private fun getLocal(jobId: Long): MutableMap<Long, ExecutionState> {
    return localJobStatesMap.getOrPut(jobId) {
      getInitialState(jobId)
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
            true,
          )
      }.toMutableMap()
  }

  /**
   * If the state of all execution completed, it's highly probable it is not needed anymore, so we can clean it up.
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
        }
      }
    } else {
      val toRemove =
        localJobStatesMap
          .filter {
            it.value.all { (_, state) -> state.status.completed }
          }.keys
      localJobStatesMap.keys.removeAll(toRemove)
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
