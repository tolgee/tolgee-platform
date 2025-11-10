package io.tolgee.batch

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.data.AllIncompleteJobsResult
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.JobUnlockedChunk
import io.tolgee.batch.events.OnBatchJobCreated
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ALLOCATION_SIZE
import io.tolgee.model.Project
import io.tolgee.model.SEQUENCE_NAME
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.batch.IBatchJob
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.BatchJobView
import io.tolgee.repository.BatchJobRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.security.SecurityService
import io.tolgee.util.Logging
import io.tolgee.util.SequenceIdProvider
import io.tolgee.util.addMinutes
import io.tolgee.util.flushAndClear
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.apache.commons.codec.digest.DigestUtils.sha256Hex
import org.hibernate.LockOptions
import org.postgresql.util.PGobject
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import java.sql.Timestamp
import java.time.Duration
import java.util.Date

@Service
class BatchJobService(
  private val batchJobRepository: BatchJobRepository,
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val cachingBatchJobService: CachingBatchJobService,
  @Lazy
  private val progressManager: ProgressManager,
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
  private val currentDateProvider: CurrentDateProvider,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade,
  private val objectMapper: ObjectMapper,
  private val jdbcTemplate: JdbcTemplate,
) : Logging {
  companion object {
    private const val DEBOUNCE_MAX_WAITING_TIME_MULTIPLIER = 4
  }

  @Transactional
  fun startJob(
    request: Any,
    project: Project? = null,
    author: UserAccount? = null,
    type: BatchJobType,
    isHidden: Boolean = false,
    debounceDuration: Duration? = null,
    debouncingKeyProvider: ((BatchOperationParams) -> Any)? = null,
  ): BatchJob {
    val processor = getProcessor(type)
    val target = processor.getTarget(request)

    val params =
      BatchOperationParams(
        projectId = project?.id,
        type = type,
        request = request,
        target = target,
      )

    val debouncingKey =
      debounceDuration?.let { getDebouncingKeySha(debouncingKeyProvider?.invoke(params) ?: params) }
    if (debouncingKey != null) {
      val debouncedJob = tryDebounceJob(debouncingKey, debounceDuration)
      if (debouncedJob != null) {
        return debouncedJob
      }
    }

    val job =
      BatchJob().apply {
        this.project = project
        this.author = author
        this.target = target
        this.totalItems = target.size
        this.chunkSize = processor.getChunkSize(projectId = project?.id, request = request)
        this.jobCharacter = processor.getJobCharacter()
        this.maxPerJobConcurrency = processor.getMaxPerJobConcurrency()
        this.type = type
        this.hidden = isHidden
        this.debounceDurationInMs = debounceDuration?.toMillis()
        this.debounceMaxWaitTimeInMs = debounceDuration?.let { it.toMillis() * DEBOUNCE_MAX_WAITING_TIME_MULTIPLIER }
        this.debouncingKey = debouncingKey
      }

    val chunked = job.chunkedTarget
    job.totalChunks = chunked.size
    cachingBatchJobService.saveJob(job)

    job.params = processor.getParams(request)

    entityManager.flushAndClear()

    val executions = storeExecutions(chunked = chunked, job = job, executeAfter = processor.getExecuteAfter(request))

    applicationContext.publishEvent(OnBatchJobCreated(job, executions))

    return job
  }

  private fun storeExecutions(
    chunked: List<List<Any>>,
    job: BatchJob,
    executeAfter: Date?,
  ): List<BatchJobChunkExecution> {
    val executions =
      List(chunked.size) { chunkNumber ->
        BatchJobChunkExecution().apply {
          batchJob = job
          this.chunkNumber = chunkNumber
          this.executeAfter = executeAfter
        }
      }

    insertExecutionsViaBatchStatement(executions)

    entityManager.clear()

    return executions
  }

  private fun insertExecutionsViaBatchStatement(executions: List<BatchJobChunkExecution>) {
    val sequenceIdProvider = SequenceIdProvider(SEQUENCE_NAME, ALLOCATION_SIZE)
    jdbcTemplate.batchUpdate(
      """
        insert into tolgee_batch_job_chunk_execution 
        (id, batch_job_id, chunk_number, status, created_at, updated_at, success_targets, execute_after) 
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """,
      executions,
      100,
    ) { ps, execution ->
      val id = sequenceIdProvider.next(ps.connection)
      execution.id = id
      ps.setLong(1, id)
      ps.setLong(2, execution.batchJob.id)
      ps.setInt(3, execution.chunkNumber)
      ps.setString(4, execution.status.name)
      ps.setTimestamp(5, Timestamp(currentDateProvider.date.time))
      ps.setTimestamp(6, Timestamp(currentDateProvider.date.time))
      ps.setObject(
        7,
        PGobject().apply {
          type = "jsonb"
          value = objectMapper.writeValueAsString(execution.successTargets)
        },
      )
      ps.setTimestamp(8, execution.executeAfter?.time?.let { Timestamp(it) })
    }
  }

  private fun tryDebounceJob(
    debouncingKey: String?,
    debounceDuration: Duration?,
  ): BatchJob? {
    debouncingKey ?: return null
    val job = batchJobRepository.findBatchJobByDebouncingKey(debouncingKey) ?: return null
    job.lastDebouncingEvent = currentDateProvider.date
    job.debounceDurationInMs = debounceDuration?.toMillis()
    cachingBatchJobService.saveJob(job)
    return job
  }

  private fun getDebouncingKeySha(key: Any): String {
    val debouncingKeyJson =
      objectMapper.writeValueAsString(key)
    return sha256Hex(debouncingKeyJson)
  }

  @TransactionalEventListener
  fun onCreated(event: OnBatchJobCreated) {
    val (job, executions) = event
    applicationContext.publishEvent(OnBatchJobCreated(job, executions))

    executions.let { batchJobChunkExecutionQueue.addToQueue(it) }
    logger.debug(
      "Starting job ${job.id}, aadded ${executions.size} executions to queue ${
        System.identityHashCode(
          batchJobChunkExecutionQueue,
        )
      }",
    )
  }

  fun findJobEntity(id: Long): BatchJob? {
    return batchJobRepository.findById(id).orElse(null)
  }

  fun getJobEntity(id: Long): BatchJob {
    return findJobEntity(id) ?: throw NotFoundException(Message.BATCH_JOB_NOT_FOUND)
  }

  fun findJobDto(id: Long): BatchJobDto? {
    return cachingBatchJobService.findJobDto(id)
  }

  fun getJobDto(id: Long): BatchJobDto {
    return this.findJobDto(id) ?: throw NotFoundException(Message.BATCH_JOB_NOT_FOUND)
  }

  fun getViews(
    projectId: Long,
    userAccount: UserAccountDto?,
    pageable: Pageable,
  ): Page<BatchJobView> {
    val jobs = batchJobRepository.getJobs(projectId, userAccount?.id, pageable)

    val progresses = getProgresses(jobs)
    val errorMessages = getErrorMessages(jobs)

    return jobs.map {
      BatchJobView(it, progresses[it.id] ?: 0, errorMessages[it.id])
    }
  }

  fun getCurrentJobViews(projectId: Long): List<BatchJobView> {
    val jobs: List<BatchJob> =
      batchJobRepository.getCurrentJobs(
        projectId,
        userAccountId = getUserAccountIdForCurrentJobView(projectId),
        oneHourAgo = currentDateProvider.date.addMinutes(-60),
        completedStatuses = BatchJobStatus.entries.filter { it.completed },
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
      if (authenticationFacade.isProjectApiKeyAuth) {
        throw e
      }
      authenticationFacade.authenticatedUser.id
    }
  }

  fun getErrorMessages(jobs: Iterable<IBatchJob>): Map<Long, Message> {
    val needsErrorMessage = jobs.filter { it.status == BatchJobStatus.FAILED }.map { it.id }.toList()

    return batchJobRepository
      .getErrorMessages(needsErrorMessage)
      .groupBy { it.batchJobId }
      .mapValues { it.value.maxBy { value -> value.updatedAt }.errorMessage }
  }

  private fun getProgresses(jobs: Iterable<BatchJob>): Map<Long, Int> {
    val cachedProgresses =
      jobs.associate {
        it.id to
          if (it.status == BatchJobStatus.RUNNING) {
            progressManager.getJobCachedProgress(jobId = it.id)
          } else {
            null
          }
      }
    val needsProgress = cachedProgresses.filter { it.value == null }.map { it.key }.toList()
    val progresses =
      batchJobRepository
        .getProgresses(needsProgress)
        .associate { it[0] as Long to it[1] as Long }

    return jobs.associate { it.id to (cachedProgresses[it.id] ?: progresses[it.id] ?: 0).toInt() }
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

  fun getAllUnlockedChunksForJobs(jobIds: Iterable<Long>): List<JobUnlockedChunk> {
    return entityManager
      .createQuery(
        """
          select new io.tolgee.batch.data.JobUnlockedChunk(bjce.batchJob.id, bjce.id)
          from BatchJobChunkExecution bjce
          where bjce.batchJob.id in :jobIds
      """,
        JobUnlockedChunk::class.java,
      ).setParameter("jobIds", jobIds)
      .setHint(
        "jakarta.persistence.lock.timeout",
        LockOptions.SKIP_LOCKED,
      ).resultList
  }

  fun getProcessor(type: BatchJobType): ChunkProcessor<Any, Any, Any> =
    applicationContext.getBean(type.processor.java) as ChunkProcessor<Any, Any, Any>

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
    entityManager
      .createQuery(
        """
        delete from BatchJobChunkExecution e where e.id in :executionIds
        """.trimIndent(),
      ).setParameter("executionIds", executions.map { it.id })
      .executeUpdate()
  }

  private fun setActivityRevisionFieldsToNull(
    batchJobIds: List<Long>,
    executionIds: List<Long>,
  ) {
    entityManager
      .createQuery(
        """
        update ActivityRevision ar set ar.batchJob = null, ar.batchJobChunkExecution = null
        where ar.batchJob.id in :batchJobIds or ar.batchJobChunkExecution.id in :executionIds
        """.trimIndent(),
      ).setParameter("batchJobIds", batchJobIds)
      .setParameter("executionIds", executionIds)
      .executeUpdate()
  }

  private fun findAllExecutionsByBatchJobIdIn(jobIds: List<Long>): List<BatchJobChunkExecution> {
    return entityManager
      .createQuery(
        """
        from BatchJobChunkExecution e
        where e.batchJob.id in :jobIds
        """.trimIndent(),
        BatchJobChunkExecution::class.java,
      ).setParameter("jobIds", jobIds)
      .resultList
  }

  fun getAllByProjectId(projectId: Long): List<BatchJob> {
    return batchJobRepository.findAllByProjectId(projectId)
  }

  fun getExecutions(batchJobId: Long): List<BatchJobChunkExecution> {
    return entityManager
      .createQuery(
        """
        from BatchJobChunkExecution e
        where e.batchJob.id = :id
        """.trimIndent(),
        BatchJobChunkExecution::class.java,
      ).setParameter("id", batchJobId)
      .resultList
  }

  fun getAllIncompleteJobIds(projectId: Long): List<AllIncompleteJobsResult> {
    return entityManager
      .createQuery(
        """select new io.tolgee.batch.data.AllIncompleteJobsResult(j.id, j.status, j.totalChunks) from BatchJob j
      where j.project.id = :projectId
      and j.status not in :completedStatuses
    """,
        AllIncompleteJobsResult::class.java,
      ).setParameter("projectId", projectId)
      .setParameter("completedStatuses", BatchJobStatus.entries.filter { it.completed })
      .resultList
  }

  fun getExecution(id: Long): BatchJobChunkExecution {
    return entityManager
      .createQuery(
        """from BatchJobChunkExecution bjce
      join fetch bjce.batchJob bk
      where bjce.id = :id
    """,
        BatchJobChunkExecution::class.java,
      ).setParameter("id", id)
      .singleResult ?: throw NotFoundException()
  }

  fun getJobsCompletedBefore(
    lockedJobIds: Iterable<Long>,
    before: Date,
  ): List<BatchJob> = batchJobRepository.getCompletedJobs(lockedJobIds, before)

  fun getStuckJobIds(jobIds: MutableSet<Long>): List<Long> {
    return batchJobRepository.getStuckJobIds(jobIds, currentDateProvider.date.addMinutes(-2))
  }

  fun save(entity: BatchJob): BatchJob {
    return cachingBatchJobService.saveJob(entity)
  }
}
