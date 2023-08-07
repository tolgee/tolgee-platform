package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.util.Logging
import io.tolgee.util.logger
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
  private val batchJobStateProvider: BatchJobStateProvider
) : Logging {
  companion object {
    private val localProjectLocks by lazy {
      ConcurrentHashMap<Long, Long?>()
    }
  }

  fun canRunBatchJobOfExecution(batchJobId: Long): Boolean {
    val jobDto = batchJobService.getJobDto(batchJobId)
    return tryLockJobForProject(jobDto)
  }

  private fun tryLockJobForProject(jobDto: BatchJobDto): Boolean {
    logger.debug("Trying to lock job ${jobDto.id} for project ${jobDto.projectId}")
    return if (usingRedisProvider.areWeUsingRedis) {
      tryLockWithRedisson(jobDto)
    } else {
      tryLockLocal(jobDto)
    }
  }

  fun unlockJobForProject(projectId: Long) {
    logger.debug("Unlocking project $projectId")
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
    }
    return localProjectLocks[projectId]
  }

  private fun tryLockLocal(toLock: BatchJobDto): Boolean {
    val computed = localProjectLocks.compute(toLock.projectId) { _, value ->
      val newLocked = computeFnBody(toLock, value)
      logger.debug("While trying to lock ${toLock.id} for project ${toLock.projectId} new lock value is $newLocked")
      newLocked
    }
    return computed == toLock.id
  }

  private fun computeFnBody(toLock: BatchJobDto, currentValue: Long?): Long {
    // nothing is locked
    if (currentValue == 0L) {
      logger.debug("Locking job ${toLock.id} for project ${toLock.projectId}, nothing is locked")
      return toLock.id
    }

    // value for the project is not initialized yet
    if (currentValue == null) {
      logger.debug("Getting initial locked state from DB state")
      // we have to find out from database if there is any running job for the project
      val initial = getInitialJobId(toLock.projectId)
      logger.debug("Initial locked job $initial for project ${toLock.projectId}")
      if (initial == null) {
        logger.debug("No job found, locking ${toLock.id}")
        return toLock.id
      }

      logger.debug("Job found, locking $initial")
      return initial
    }

    logger.debug("Job $currentValue is locked for project ${toLock.projectId}")
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

  /**
   * It can happen that some other thread or instance will try to
   * execute execution of already completed job
   *
   * The execution is skipped, since it's not pending, but
   * we have to unlock the project, otherwise it will be locked forever
   */
  fun unlockJobIfCompleted(jobId: Long) {
    val cached = batchJobStateProvider.getCached(jobId)
    logger.debug("Checking if job $jobId is completed, has cached value: ${cached != null}")
    val isCompleted = cached?.all { it.value.status.completed } ?: true
    if (isCompleted) {
      logger.debug("Job $jobId is completed, unlocking project")
      val jobDto = batchJobService.getJobDto(jobId)
      unlockJobForProject(jobDto.projectId)
    }
  }
}
