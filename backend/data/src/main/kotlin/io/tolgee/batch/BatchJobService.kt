package io.tolgee.batch

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.views.BatchJobView
import io.tolgee.repository.BatchJobRepository
import io.tolgee.util.executeInNewTransaction
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
  @Lazy
  private val batchJobChunkService: BatchJobActionService,
  private val transactionManager: PlatformTransactionManager,
  private val cachingBatchJobService: CachingBatchJobService,
  @Lazy
  private val progressManager: ProgressManager
) {

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

    executions?.forEach { batchJobChunkService.addToQueue(it) }
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
    val cachedProgresses =
      jobs.map {
        it.id to
          if (it.status == BatchJobStatus.RUNNING)
            progressManager.getJobCachedProgress(jobId = it.id)
          else
            null
      }.toMap()
    val needsProgress = cachedProgresses.filter { it.value == null }.map { it.key }.toList()
    val progresses = batchJobRepository.getProgresses(needsProgress)
      .associate { (it[0] as BigInteger).toLong() to it[1] as BigInteger }
    return jobs.map {
      val progress = cachedProgresses[it.id] ?: progresses[it.id]?.toLong() ?: 0
      BatchJobView(it, progress.toInt())
    }
  }

  fun getView(jobId: Long): BatchJobView {
    val job = batchJobRepository.findById(jobId).orElseThrow { NotFoundException() }
    val cachedProgress = if (job.status == BatchJobStatus.RUNNING)
      progressManager.getJobCachedProgress(jobId = job.id)
    else
      null
    val progress = cachedProgress
      ?: (batchJobRepository.getProgresses(listOf(jobId)).singleOrNull()?.get(1) as? BigInteger)?.toLong() ?: 0

    return BatchJobView(job, progress.toInt())
  }

  @Suppress("UNCHECKED_CAST")
  fun <RequestType> getProcessor(type: BatchJobType): ChunkProcessor<RequestType> =
    applicationContext.getBean(type.processor.java) as ChunkProcessor<RequestType>
}
