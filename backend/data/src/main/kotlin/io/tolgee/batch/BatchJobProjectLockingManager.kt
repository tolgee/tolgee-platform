package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.component.UsingRedisProvider
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

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
) : Logging {
  companion object {
    private val localProjectLocks by lazy {
      ConcurrentHashMap<Long, Long?>()
    }
  }

  fun canLockJobForProject(batchJobId: Long): Boolean {
    val jobDto = batchJobService.getJobDto(batchJobId)
    if (!jobDto.type.exclusive) {
      return true
    }
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

  fun unlockJobForProject(
    projectId: Long?,
    jobId: Long,
  ) {
    projectId ?: return
    getMap().compute(projectId) { _, lockedJobId ->
      logger.debug("Unlocking job: $jobId for project $projectId")
      if (lockedJobId == jobId) {
        logger.debug("Unlocking job: $jobId for project $projectId")
        return@compute 0L
      }
      logger.debug("Job: $jobId for project $projectId is not locked")
      return@compute lockedJobId
    }
  }

  fun getMap(): ConcurrentMap<Long, Long?> {
    if (usingRedisProvider.areWeUsingRedis) {
      return getRedissonProjectLocks()
    }
    return localProjectLocks
  }

  private fun tryLockWithRedisson(batchJobDto: BatchJobDto): Boolean {
    val projectId = batchJobDto.projectId ?: return true
    val computed =
      getRedissonProjectLocks().compute(projectId) { _, value ->
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
    val projectId = toLock.projectId ?: return true
    val computed =
      localProjectLocks.compute(projectId) { _, value ->
        val newLocked = computeFnBody(toLock, value)
        logger.debug("While trying to lock ${toLock.id} for project ${toLock.projectId} new lock value is $newLocked")
        newLocked
      }
    return computed == toLock.id
  }

  private fun computeFnBody(
    toLock: BatchJobDto,
    currentValue: Long?,
  ): Long {
    val projectId =
      toLock.projectId
        ?: throw IllegalStateException(
          "Project id is required. " +
            "Locking for project should not happen for non-project jobs.",
        )
    // nothing is locked
    if (currentValue == 0L) {
      logger.debug("Locking job ${toLock.id} for project ${toLock.projectId}, nothing is locked")
      return toLock.id
    }

    // value for the project is not initialized yet
    if (currentValue == null) {
      logger.debug("Getting initial locked state from DB state")
      // we have to find out from database if there is any running job for the project
      val initial = getInitialJobId(projectId)
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
    val jobs = batchJobService.getAllIncompleteJobIds(projectId)

    // First priority: Find actually RUNNING jobs from database
    // This prevents phantom locks from PENDING jobs that have chunks in queue but aren't executing
    val runningJob =
      jobs.find { incompleteJob ->
        incompleteJob.status == io.tolgee.model.batch.BatchJobStatus.RUNNING
      }
    if (runningJob != null) {
      logger.debug("Found RUNNING job ${runningJob.jobId} for project $projectId")
      return runningJob.jobId
    }

    // Fallback: Use original logic for jobs that have started processing
    val unlockedChunkCounts =
      batchJobService
        .getAllUnlockedChunksForJobs(jobs.map { it.jobId })
        .groupBy { it.batchJobId }
        .map { it.key to it.value.count() }
        .toMap()
    // we are looking for a job that has already started and preferably for a locked one
    val startedJob = jobs.find { it.totalChunks != unlockedChunkCounts[it.jobId] }
    if (startedJob != null) {
      logger.debug("Found started job ${startedJob.jobId} for project $projectId (fallback logic)")
      return startedJob.jobId
    }

    logger.debug("No RUNNING or PENDING jobs found for project $projectId, allowing new job to acquire lock")
    return null
  }

  private fun getRedissonProjectLocks(): RMap<Long, Long> {
    return redissonClient.getMap("project_batch_job_locks")
  }

  fun getLockedJobIds(): Set<Long> {
    return getMap().values.filterNotNull().toSet()
  }
}
