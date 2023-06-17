package io.tolgee.batch

import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.repository.BatchJobRepository
import io.tolgee.util.executeInNewTransaction
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import javax.persistence.EntityManager

@Service
class BatchJobService(
  private val batchJobRepository: BatchJobRepository,
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val batchJobChunkService: BatchJobActionService,
  private val transactionManager: PlatformTransactionManager
) {
  fun <RequestType> startJob(request: RequestType, author: UserAccount?, type: BatchJobType): BatchJob {
    var executions: List<BatchJobChunkExecution>? = null
    val job = executeInNewTransaction(transactionManager) {
      val processor = getProcessor<RequestType>(type)
      val target = processor.getTarget(request)

      val chunked = target.chunked(BatchJobType.TRANSLATION.chunkSize)

      val job = createJob(author, target, chunked, BatchJobType.TRANSLATION.chunkSize)

      val params = processor.getParams(request, job)

      batchJobRepository.save(job)
      entityManager.persist(params)

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

  private fun createJob(
    author: UserAccount?,
    target: List<Long>,
    chunked: List<List<Long>>,
    chunkSize: Int
  ): BatchJob {
    return BatchJob().apply {
      this.author = author
      this.target = target
      this.totalChunks = chunked.size
      this.totalItems = target.size
      this.chunkSize = chunkSize
    }
  }

  fun findJob(id: Long): BatchJob? {
    return batchJobRepository.findById(id).orElse(null)
  }

  fun getJob(id: Long): BatchJob {
    return findJob(id) ?: throw NotFoundException(io.tolgee.constants.Message.BATCH_JOB_NOT_FOUND)
  }

  @Suppress("UNCHECKED_CAST")
  fun <RequestType> getProcessor(type: BatchJobType): ChunkProcessor<RequestType> =
    applicationContext.getBean(type.processor.java) as ChunkProcessor<RequestType>
}
