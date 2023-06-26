package io.tolgee.batch

import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.repository.BatchJobRepository
import io.tolgee.util.executeInNewTransaction
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class BatchJobService(
  private val batchJobRepository: BatchJobRepository,
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  @Lazy
  private val batchJobChunkService: BatchJobActionService,
  private val transactionManager: PlatformTransactionManager,
  private val cachingBatchJobService: CachingBatchJobService
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

  @Suppress("UNCHECKED_CAST")
  fun <RequestType> getProcessor(type: BatchJobType): ChunkProcessor<RequestType> =
    applicationContext.getBean(type.processor.java) as ChunkProcessor<RequestType>
}
