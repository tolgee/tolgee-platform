package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobProjectLockingManager
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.JobCharacter
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.openApiDocs.OpenApiSelfHostedExtension
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST API for managing project batch job locks
 */
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/administration")
@Tag(
  name = "Server Administration",
  description = "**Only for self-hosted instances** \n\n" +
    "Management of project-level batch job locks and queue inspection for debugging and maintenance."
)
@OpenApiSelfHostedExtension
class ProjectBatchLockController(
  private val batchJobProjectLockingManager: BatchJobProjectLockingManager,
  private val batchJobService: BatchJobService,
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
) : IController, Logging {

  @GetMapping("/project-batch-locks")
  @Operation(
    summary = "Get all project batch locks",
    description = "Returns current project batch job locks from Redis or local storage based on configuration"
  )
  @RequiresSuperAuthentication
  fun getProjectLocks(): CollectionModel<ProjectLockModel> {
    logger.debug("Retrieving all project batch locks")
    
    val locks = batchJobProjectLockingManager.getMap()
    val lockModels = locks.entries.map { (projectId, lockedJobIds) ->
      createProjectLockModel(projectId, lockedJobIds)
    }

    logger.debug("Retrieved ${lockModels.size} project batch locks")
    return CollectionModel.of(lockModels)
  }

  private fun createProjectLockModel(projectId: Long, lockedJobIds: Set<Long>): ProjectLockModel {
    val lockStatus = when {
      lockedJobIds.isEmpty() -> LockStatus.UNLOCKED
      else -> LockStatus.LOCKED
    }

    val jobInfos = lockedJobIds.mapNotNull { jobId ->
      try {
        val jobDto = batchJobService.getJobDto(jobId)
        JobInfo(
          jobId = jobDto.id,
          status = jobDto.status,
          type = jobDto.type,
          createdAt = jobDto.createdAt
        )
      } catch (e: Exception) {
        logger.warn("Could not retrieve job info for locked job $jobId in project $projectId", e)
        null
      }
    }

    return ProjectLockModel(
      projectId = projectId,
      lockedJobIds = lockedJobIds,
      lockStatus = lockStatus,
      jobInfos = jobInfos
    )
  }

  @GetMapping("/batch-job-queue")
  @Operation(
    summary = "Get current batch job queue",
    description = "Returns all chunk execution items currently in the batch job queue"
  )
  @RequiresSuperAuthentication
  fun getBatchJobQueue(): CollectionModel<QueueItemModel> {
    logger.debug("Retrieving current batch job queue")
    
    val queueItems = batchJobChunkExecutionQueue.getAllQueueItems()
    val queueModels = queueItems.map { item ->
      QueueItemModel(
        chunkExecutionId = item.chunkExecutionId,
        jobId = item.jobId,
        executeAfter = item.executeAfter,
        jobCharacter = item.jobCharacter,
        managementErrorRetrials = item.managementErrorRetrials
      )
    }
    
    logger.debug("Retrieved ${queueModels.size} items from batch job queue")
    return CollectionModel.of(queueModels)
  }
}

/**
 * Model representing a project batch lock
 */
data class ProjectLockModel(
  val projectId: Long,
  val lockedJobIds: Set<Long>,
  val lockStatus: LockStatus,
  val jobInfos: List<JobInfo>
)

/**
 * Information about the locked job
 */
data class JobInfo(
  val jobId: Long,
  val status: BatchJobStatus,
  val type: io.tolgee.batch.data.BatchJobType,
  val createdAt: Long?
)

/**
 * Status of the project lock
 */
enum class LockStatus {
  /** Project lock is explicitly cleared (value = empty set) */
  UNLOCKED,

  /** Project lock is held by one or more jobs (value = set of job IDs) */
  LOCKED
}

/**
 * Model representing a queue item for batch job chunk execution
 */
data class QueueItemModel(
  val chunkExecutionId: Long,
  val jobId: Long,
  val executeAfter: Long?,
  val jobCharacter: JobCharacter,
  val managementErrorRetrials: Int
)
