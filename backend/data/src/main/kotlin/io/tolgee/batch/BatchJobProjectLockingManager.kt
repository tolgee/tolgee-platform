package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.component.UsingRedisProvider
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private const val REDIS_PROJECT_BATCH_JOB_LOCKS_KEY = "project_batch_job_locks"

/**
 * Only single job can be executed at the same time for one project.
 *
 * This class handles that
 */
@Component
class BatchJobProjectLockingManager(
  private val batchJobService: BatchJobService,
  private val batchProperties: BatchProperties,
  @Lazy
  private val redissonClient: RedissonClient,
  private val usingRedisProvider: UsingRedisProvider,
) : Logging, InitializingBean {
  companion object {
    private val localProjectLocks by lazy {
      ConcurrentHashMap<Long, Set<Long>>()
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
    getMap().compute(projectId) { _, lockedJobIds ->
      val currentJobs = lockedJobIds ?: emptySet()
      if (currentJobs.contains(jobId)) {
        logger.debug("Unlocking job: $jobId for project $projectId")
        val updatedJobs = currentJobs - jobId
        return@compute updatedJobs.ifEmpty { emptySet() }
      }
      logger.debug("Job: $jobId for project $projectId is not locked")
      return@compute currentJobs
    }
  }

  fun getMap(): ConcurrentMap<Long, Set<Long>> {
    if (usingRedisProvider.areWeUsingRedis) {
      return getRedissonProjectLocks()
    }
    return localProjectLocks
  }

  private fun tryLockWithRedisson(batchJobDto: BatchJobDto): Boolean {
    val projectId = batchJobDto.projectId ?: return true
    val computedJobIds =
      getRedissonProjectLocks().compute(projectId) { _, lockedJobIds ->
        val newLockedJobIds = computeLockedJobIdsForProject(batchJobDto, lockedJobIds ?: emptySet())
        logger.debug(
          "While trying to lock on redis {} for project {} new lock value is {}",
          batchJobDto.id,
          batchJobDto.projectId,
          newLockedJobIds
        )
        newLockedJobIds
      } ?: emptySet()
    return computedJobIds.contains(batchJobDto.id)
  }

  fun getLockedForProject(projectId: Long): Set<Long> {
    if (usingRedisProvider.areWeUsingRedis) {
      return getRedissonProjectLocks()[projectId] ?: emptySet()
    }
    return localProjectLocks[projectId] ?: emptySet()
  }

  private fun tryLockLocal(batchJobDto: BatchJobDto): Boolean {
    val projectId = batchJobDto.projectId ?: return true
    val computedJobIds =
      localProjectLocks.compute(projectId) { _, lockedJobIds ->
        val newLockedJobIds = computeLockedJobIdsForProject(batchJobDto, lockedJobIds ?: emptySet())
        logger.debug(
          "While trying to lock locally {} for project {} new lock value is {}",
          batchJobDto.id,
          batchJobDto.projectId,
          newLockedJobIds
        )
        newLockedJobIds
      } ?: emptySet()
    return computedJobIds.contains(batchJobDto.id)
  }

  private fun computeLockedJobIdsForProject(
    toLock: BatchJobDto,
    lockedJobIds: Set<Long>,
  ): Set<Long> {
    val projectId =
      toLock.projectId
        ?: throw IllegalStateException(
          "Project id is required. " +
            "Locking for project should not happen for non-project jobs.",
        )

    // nothing is locked
    if (lockedJobIds.isEmpty()) {
      logger.debug("Getting initial locked state from DB state")
      // we have to find out from database if there is any running job for the project
      val initialJobId = getInitialJobId(projectId)
      logger.info("Initial locked job $initialJobId for project ${toLock.projectId}")
      if (initialJobId == null) {
        logger.debug("No initial job found, locking only ${toLock.id}")
        return setOf(toLock.id)
      }
      val newLockedJobIds = mutableSetOf<Long>(initialJobId)
      if (newLockedJobIds.size < batchProperties.projectConcurrency) {
        logger.debug("Locking job ${toLock.id} for project $projectId. Active jobs before: $newLockedJobIds")
        newLockedJobIds.add(toLock.id)
      } else {
        logger.debug(
          "Cannot lock job ${toLock.id} for project $projectId, limit reached. Active jobs: $newLockedJobIds"
        )
      }
      return newLockedJobIds
    }

    // standard case - there are some jobs locked
    if (lockedJobIds.size < batchProperties.projectConcurrency) {
      logger.debug("Locking job ${toLock.id} for project $projectId. Active jobs before: $lockedJobIds")
      return lockedJobIds + toLock.id
    }

    // if we cannot lock, we are returning current value
    return lockedJobIds
  }

  private fun getInitialJobId(projectId: Long): Long? {
    val jobs = batchJobService.getAllIncompleteJobIds(projectId)

    // First priority: Find actually RUNNING jobs from database
    // This prevents phantom locks from PENDING jobs that have chunks in queue but aren't executing
    val runningJob = jobs.find { incompleteJob ->
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
        .groupBy { it.batchJobId }.map { it.key to it.value.count() }.toMap()
    // we are looking for a job that has already started and preferably for a locked one
    val startedJob = jobs.find { it.totalChunks != unlockedChunkCounts[it.jobId] }
    if (startedJob != null) {
      logger.debug("Found started job ${startedJob.jobId} for project $projectId (fallback logic)")
      return startedJob.jobId
    }

    logger.debug("No RUNNING or PENDING jobs found for project $projectId, allowing new job to acquire lock")
    return null
  }

  private fun getRedissonProjectLocks(): ConcurrentMap<Long, Set<Long>> {
      return redissonClient.getMap(REDIS_PROJECT_BATCH_JOB_LOCKS_KEY)
    }

  override fun afterPropertiesSet() {
    // This runs first to check if redis has a map of the old format.
    // If so, we migrate it to the new format.
    if (!usingRedisProvider.areWeUsingRedis) {
      logger.debug("Not using Redis, skipping migration check")
      return
    }

    val redisProjectBatchJobLocks = redissonClient.getMap<Long, Any>(REDIS_PROJECT_BATCH_JOB_LOCKS_KEY)
    val isOldFormat = redisProjectBatchJobLocks.values.any { it is Long || it == null }
    if (!isOldFormat) {
      logger.debug("Redis project locks are in new format, no migration needed")
      return
    }

    logger.info("Starting migration of project locks from old format (v1) to new format (v2)")
    // First, copy all data from Redis to local memory
    val localCopy = mutableMapOf<Long, Set<Long>>()
    redisProjectBatchJobLocks.forEach { (projectId, jobId) ->
      val jobSet = when (jobId) {
        null, 0L -> emptySet<Long>()
        else -> setOf<Long>(jobId as Long)
      }
      localCopy[projectId] = jobSet
    }
    logger.info("Copied ${localCopy.size} project locks from old format to local memory")

    // Write all data back in new format (this will overwrite the old format)
    val newMap = getRedissonProjectLocks()
    localCopy.forEach { (projectId, jobSet) ->
      newMap[projectId] = jobSet
    }

    logger.info("Successfully migrated ${newMap.size} project locks from local memory to new format")
  }

  fun getLockedJobIds(): Set<Long> {
    return getMap().values.flatten().toSet()
  }
}
