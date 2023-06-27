package io.tolgee.batch.state

import io.tolgee.component.LockingProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
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
) {
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

  fun get(jobId: Long): MutableMap<Long, ExecutionState> {
    return if (usingRedisProvider.areWeUsingRedis) {
      getRedissonMap(jobId)
    } else {
      getLocalMap(jobId)
    }
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
