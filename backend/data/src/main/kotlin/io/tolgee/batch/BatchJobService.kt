package io.tolgee.batch

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.batch.IBatchJob
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.BatchJobView
import io.tolgee.repository.BatchJobRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.security.SecurityService
import io.tolgee.util.Logging
import io.tolgee.util.addMinutes
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import javax.persistence.EntityManager

@Service
class BatchJobService(
  private val batchJobRepository: BatchJobRepository,
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val transactionManager: PlatformTransactionManager,
  private val cachingBatchJobService: CachingBatchJobService,
  @Lazy
  private val progressManager: ProgressManager,
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
  private val currentDateProvider: CurrentDateProvider,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade
) : Logging {

  @Transactional
  fun <RequestType> startJob(
    request: RequestType,
    project: Project,
    author: UserAccount?,
    type: BatchJobType
  ): BatchJob {
    var executions: List<BatchJobChunkExecution>? = null
    val job = executeInNewTransaction(transactionManager) {
      val processor = getProcessor<RequestType>(type)
      val target = processor.getTarget(request)

      val job = BatchJob().apply {
        this.project = project
        this.author = author
        this.target = target
        this.totalItems = target.size
        this.chunkSize = type.chunkSize
        this.type = type
      }
      val chunked = job.chunkedTarget
      job.totalChunks = chunked.size
      cachingBatchJobService.saveJob(job)

      val params = processor.getParams(request, job)

      params?.let {
        entityManager.persist(params)
      }

      executions = chunked.mapIndexed { chunkNumber, _ ->
        BatchJobChunkExecution().apply {
          batchJob = job
          this.chunkNumber = chunkNumber
          entityManager.persist(this)
        }
      }
      job
    }

    executions?.let { batchJobChunkExecutionQueue.addToQueue(it) }
    logger.debug(
      "Starting job ${job.id}, aadded ${executions?.size} executions to queue ${
      System.identityHashCode(
        batchJobChunkExecutionQueue
      )
      }"
    )

    return job
  }

  fun findJobEntity(id: Long): BatchJob? {
    return batchJobRepository.findById(id).orElse(null)
  }

  fun getJobEntity(id: Long): BatchJob {
    return findJobEntity(id) ?: throw NotFoundException(io.tolgee.constants.Message.BATCH_JOB_NOT_FOUND)
  }

  fun findJobDto(id: Long): BatchJobDto? {
    return cachingBatchJobService.findJobDto(id)
  }

  fun getJobDto(id: Long): BatchJobDto {
    return this.findJobDto(id) ?: throw NotFoundException(io.tolgee.constants.Message.BATCH_JOB_NOT_FOUND)
  }

  fun getViews(projectId: Long, userAccount: UserAccountDto?, pageable: Pageable): Page<BatchJobView> {
    val jobs = batchJobRepository.getJobs(projectId, userAccount?.id, pageable)

    val progresses = getProgresses(jobs)
    val errorMessages = getErrorMessages(jobs)

    return jobs.map {
      BatchJobView(it, progresses[it.id] ?: 0, errorMessages[it.id])
    }
  }

  fun getCurrentJobViews(projectId: Long): List<BatchJobView> {
    val jobs: List<BatchJob> = batchJobRepository.getCurrentJobs(
      projectId,
      userAccountId = getUserAccountIdForCurrentJobView(projectId),
      oneHourAgo = currentDateProvider.date.addMinutes(-60),
      completedStatuses = BatchJobStatus.values().filter { it.completed }
    )

    val progresses = getProgresses(jobs)
    val errorMessages = getErrorMessages(jobs)

    return jobs.map {
      BatchJobView(it, progresses[it.id] ?: 0, errorMessages[it.id])
    }
  }

  /**
   * Returns user account id if user has no permission to view all jobs.
   */
  private fun getUserAccountIdForCurrentJobView(projectId: Long): Long? {

    return try {
      securityService.checkProjectPermission(projectId, Scope.BATCH_JOBS_VIEW)
      null
    } catch (e: PermissionException) {
      if (authenticationFacade.isApiKeyAuthentication) {
        throw e
      }
      authenticationFacade.userAccount.id
    }
  }

  fun getErrorMessages(jobs: Iterable<IBatchJob>): Map<Long, Message> {
    val needsErrorMessage = jobs.filter { it.status == BatchJobStatus.FAILED }.map { it.id }.toList()

    return batchJobRepository.getErrorMessages(needsErrorMessage)
      .groupBy { it.batchJobId }
      .mapValues { it.value.minBy { value -> value.updatedAt }.errorMessage }
  }

  private fun getProgresses(jobs: Iterable<BatchJob>): Map<Long, Int> {
    val cachedProgresses =
      jobs.associate {
        it.id to
          if (it.status == BatchJobStatus.RUNNING)
            progressManager.getJobCachedProgress(jobId = it.id)
          else
            null
      }
    val needsProgress = cachedProgresses.filter { it.value == null }.map { it.key }.toList()
    val progresses = batchJobRepository.getProgresses(needsProgress)
      .associate { (it[0] as BigInteger).toLong() to it[1] as BigInteger }

    return jobs.associate { it.id to (cachedProgresses[it.id] ?: progresses[it.id]?.toLong() ?: 0).toInt() }
  }

  fun getView(jobId: Long): BatchJobView {
    val job = batchJobRepository.findById(jobId).orElseThrow { NotFoundException() }
    return getView(job)
  }

  fun getView(job: BatchJob): BatchJobView {
    val progress = getProgresses(listOf(job))[job.id] ?: 0
    val errorMessage = getErrorMessages(listOf(job))[job.id]
    return BatchJobView(job, progress, errorMessage)
  }

  @Suppress("UNCHECKED_CAST")
  fun <RequestType> getProcessor(type: BatchJobType): ChunkProcessor<RequestType> =
    applicationContext.getBean(type.processor.java) as ChunkProcessor<RequestType>

  fun deleteAllByProjectId(projectId: Long) {
    val batchJobs = getAllByProjectId(projectId)
    val batchJobIds = batchJobs.map { it.id }
    val executions = findAllExecutionsByBatchJobIdIn(batchJobIds)
    val executionIds = executions.map { it.id }
    setActivityRevisionFieldsToNull(batchJobIds, executionIds)
    deleteExecutions(executions)
    batchJobRepository.deleteAll(batchJobs)
  }

  private fun deleteExecutions(executions: List<BatchJobChunkExecution>) {
    entityManager.createQuery(
      """
      delete from BatchJobChunkExecution e where e.id in :executionIds
      """.trimIndent()
    ).setParameter("executionIds", executions.map { it.id })
      .executeUpdate()
  }

  private fun setActivityRevisionFieldsToNull(batchJobIds: List<Long>, executionIds: List<Long>) {
    entityManager.createQuery(
      """
      update ActivityRevision ar set ar.batchJob = null, ar.batchJobChunkExecution = null
      where ar.batchJob.id in :batchJobIds or ar.batchJobChunkExecution.id in :executionIds
      """.trimIndent()
    ).setParameter("batchJobIds", batchJobIds)
      .setParameter("executionIds", executionIds)
      .executeUpdate()
  }

  private fun findAllExecutionsByBatchJobIdIn(jobIds: List<Long>): List<BatchJobChunkExecution> {
    return entityManager.createQuery(
      """
      from BatchJobChunkExecution e
      where e.batchJob.id in :jobIds
      """.trimIndent(),
      BatchJobChunkExecution::class.java
    )
      .setParameter("jobIds", jobIds)
      .resultList
  }

  fun getAllByProjectId(projectId: Long): List<BatchJob> {
    return batchJobRepository.findAllByProjectId(projectId)
  }

  fun deleteMultiple(ids: Iterable<Long>) {
  }
}
