package io.tolgee.batch.state

import io.tolgee.component.LockingProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.persistence.EntityManager

@Component
class BatchJobStateProvider(
  val usingRedisProvider: UsingRedisProvider,
  @Lazy
  val redissonClient: RedissonClient,
  val entityManager: EntityManager,
  val lockingProvider: LockingProvider
) : Logging {
  companion object {
    private val localJobStatesMap by lazy {
      ConcurrentHashMap<Long, MutableMap<Long, ExecutionState>>()
    }
  }

  fun <T> updateState(jobId: Long, block: (MutableMap<Long, ExecutionState>) -> T): T {
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
      transactionCommitted = false
    )
  }

  fun get(jobId: Long): MutableMap<Long, ExecutionState> {
    return if (usingRedisProvider.areWeUsingRedis) {
      getRedissonMap(jobId)
    } else {
      getLocalMap(jobId)
    }
  }

  /**
   * Doesn't init map if not exists
   */
  fun getCached(jobId: Long): MutableMap<Long, ExecutionState>? {
    return getStatesMap()[jobId]
  }

  fun removeJobState(jobId: Long): MutableMap<Long, ExecutionState>? {
    return getStatesMap().remove(jobId)
  }

  fun hasCachedJobState(jobId: Long): Boolean {
    return getStatesMap().containsKey(jobId)
  }

  fun getStatesMap(): ConcurrentMap<Long, MutableMap<Long, ExecutionState>> {
    return if (usingRedisProvider.areWeUsingRedis) {
      getRedissonStatesMap()
    } else {
      localJobStatesMap
    }
  }

  private fun getLocalMap(jobId: Long): MutableMap<Long, ExecutionState> {
    return localJobStatesMap.getOrPut(jobId) {
      getInitialState(jobId)
    }
  }

  private fun getRedissonMap(jobId: Long): MutableMap<Long, ExecutionState> {
    val statesMap = getRedissonStatesMap()

    return statesMap.getOrPut(jobId) {
      getInitialState(jobId)
    }
  }

  private fun getRedissonStatesMap(): RMap<Long, MutableMap<Long, ExecutionState>> {
    return redissonClient.getMap("batch_job_state")
  }

  fun getInitialState(jobId: Long): MutableMap<Long, ExecutionState> {
    logger.debug("Initializing batch job state for job $jobId")
    val executions = entityManager.createQuery(
      """
      from BatchJobChunkExecution bjce
      where bjce.batchJob.id = :jobId
      """,
      BatchJobChunkExecution::class.java
    )
      .setParameter("jobId", jobId).resultList

    return executions.associate {
      it.id to ExecutionState(
        it.successTargets,
        it.status,
        it.chunkNumber,
        it.retry,
        true
      )
    }.toMutableMap()
  }
}
