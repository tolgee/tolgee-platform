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
import java.util.concurrent.ConcurrentMap

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
  }

  fun <T> updateState(
    jobId: Long,
    block: (MutableMap<Long, ExecutionState>) -> T,
  ): T {
    return lockingProvider.withLocking("batch_job_state_lock_$jobId") {
      val map = get(jobId)
      val result = block(map)
      getStatesMap()[jobId] = map
      result
    }
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
    return getStatesMap()[jobId]
  }

  fun removeJobState(jobId: Long): MutableMap<Long, ExecutionState>? {
    logger.debug("Removing job state for job $jobId")
    return getStatesMap().remove(jobId)
  }

  fun hasCachedJobState(jobId: Long): Boolean {
    return getStatesMap().containsKey(jobId)
  }

  private fun getStatesMap(): ConcurrentMap<Long, MutableMap<Long, ExecutionState>> {
    if (usingRedisProvider.areWeUsingRedis) {
      return getRedissonStatesMap()
    }
    return localJobStatesMap
  }

  fun get(jobId: Long): MutableMap<Long, ExecutionState> {
    return getStatesMap().getOrPut(jobId) {
      getInitialState(jobId)
    }
  }

  private fun getRedissonStatesMap(): RMap<Long, MutableMap<Long, ExecutionState>> {
    return redissonClient.getMap("batch_job_state")
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
    val toRemove =
      getStatesMap()
        .filter {
          it.value.all { (_, state) -> state.status.completed && state.status.completed }
        }.keys
    getStatesMap().keys.removeAll(toRemove)
  }

  fun getCachedJobIds(): MutableSet<Long> {
    val keys = getStatesMap().keys
    // redisson defers the access to the key set, so it was throwing NoSuchElementException when iterating over keys
    // so let's rather copy the set
    return keys.toMutableSet()
  }
}
