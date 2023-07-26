package io.tolgee.batch

import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Only single job can be executed at the same time for one project.
 *
 * This class handles that
 */
@Component
class BatchJobProjectLockingManager(
  private val batchJobService: BatchJobService,
  @Lazy
  private val redissonClient: RedissonClient,
  private val usingRedisProvider: UsingRedisProvider,
) {
  companion object {
    private val localProjectLocks by lazy {
      ConcurrentHashMap<Long, Long?>()
    }
  }

  fun canRunBatchJobOfExecution(execution: BatchJobChunkExecution): Boolean {
    val jobDto = batchJobService.getJobDto(execution.batchJob.id)
    return tryLockJobForProject(jobDto)
  }

  private fun tryLockJobForProject(jobDto: BatchJobDto): Boolean {
    return if (usingRedisProvider.areWeUsingRedis) {
      tryLockWithRedisson(jobDto)
    } else {
      tryLockLocal(jobDto)
    }
  }

  fun unlockJobForProject(projectId: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      getRedissonProjectLocks()[projectId] = 0L
    } else {
      localProjectLocks[projectId] = 0L
    }
  }

  private fun tryLockWithRedisson(batchJobDto: BatchJobDto): Boolean {
    val computed = getRedissonProjectLocks().compute(batchJobDto.projectId) { _, value ->
      computeFnBody(batchJobDto, value)
    }
    return computed == batchJobDto.id
  }

  fun getLockedForProject(projectId: Long): Long? {
    if (usingRedisProvider.areWeUsingRedis) {
      return getRedissonProjectLocks()[projectId]
    } else {
      return localProjectLocks[projectId]
    }
  }

  private fun tryLockLocal(toLock: BatchJobDto): Boolean {
    val computed = localProjectLocks.compute(toLock.projectId) { _, value ->
      computeFnBody(toLock, value)
    }
    return computed == toLock.id
  }

  private fun computeFnBody(toLock: BatchJobDto, currentValue: Long?): Long {
    // nothing is locked
    if (currentValue == 0L) {
      return toLock.id
    }

    // value for the project is not initialized yet
    if (currentValue == null) {
      // we have to find out from database if there is any running job for the project
      return getInitialJobId(toLock.projectId) ?: toLock.id
    }

    // if we cannot lock, we are returning current value
    return currentValue
  }

  private fun getInitialJobId(projectId: Long): Long? {
    val jobs = batchJobService.getAllIncompleteJobs(projectId)
    val unlockedChunkCounts = batchJobService
      .getAllUnlockedChunksForJobs(jobs.map { it.id })
      .groupBy { it.batchJob.id }.map { it.key to it.value.count() }.toMap()
    return jobs.find { it.totalChunks != unlockedChunkCounts[it.id] }?.id
  }

  private fun getRedissonProjectLocks(): RMap<Long, Long> {
    return redissonClient.getMap("project_batch_job_locks")
  }
}
