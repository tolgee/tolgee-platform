package io.tolgee.service.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.dtos.BatchJobChunkMessage
import io.tolgee.dtos.request.BatchTranslateRequest
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.TranslateJobParams
import io.tolgee.repository.BatchJobRepository
import io.tolgee.util.executeInNewTransaction
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class BatchJobService(
  private val batchJobRepository: BatchJobRepository,
  private val entityManager: EntityManager,
  private val rabbitTemplate: RabbitTemplate,
  private val transactionManager: PlatformTransactionManager
) {

  companion object {
    const val CHUNK_SIZE = 10
  }

  @Transactional
  fun createTranslateJob(data: BatchTranslateRequest, author: UserAccount?): BatchJob {
    val chunked = data.keyIds.chunked(CHUNK_SIZE)

    val job = BatchJob().apply {
      this.author = author
      this.target = data.keyIds
      this.totalChunks = chunked.size
      this.totalItems = data.keyIds.size
      this.chunkSize = CHUNK_SIZE
    }

    val params = TranslateJobParams().apply {
      this.batchJob = job
      this.targetLanguageIds = data.targetLanguageIds
      this.useMachineTranslation = data.useMachineTranslation
      this.useTranslationMemory = data.useTranslationMemory
      this.service = data.service
    }

    executeInNewTransaction(transactionManager) {
      batchJobRepository.save(job)
      entityManager.persist(params)
    }

    chunked.forEachIndexed { index, chunk ->
      val json = convertMessage(BatchJobChunkMessage(job.id, index))
      rabbitTemplate.convertAndSend("tolgee-exchange", "batch-operations", json)
    }

    return job
  }

  private fun convertMessage(message: BatchJobChunkMessage): String =
    jacksonObjectMapper().writeValueAsString(message)

  @Transactional
  fun processChunk(message: BatchJobChunkMessage) {
    val job =
      batchJobRepository.findById(message.batchJobId).orElse(null)
        ?: throw IllegalStateException("Job not found")

    val chunked = job.target.chunked(job.chunkSize)
    val chunk = chunked[message.chunkNumber]

    @Suppress("UNCHECKED_CAST")
    val previousExecutions = entityManager.createQuery(
      """
      from BatchJobChunkExecution 
      where chunkNumber = :chunkNumber 
          and batchJob.id = :batchJobId
      """.trimIndent()
    )
      .setParameter("chunkNumber", message.chunkNumber)
      .setParameter("batchJobId", message.batchJobId)
      .resultList as List<BatchJobChunkExecution>

    val previousSuccessfulTargets = previousExecutions.flatMap { it.successTargets }.toSet()

    val toProcess = chunk.toMutableSet()
    toProcess.removeAll(previousSuccessfulTargets)

    val successfulTargets = mutableSetOf<Long>()
    val execution: BatchJobChunkExecution = BatchJobChunkExecution().apply {
      this.batchJob = job
      this.chunkNumber = message.chunkNumber
    }

    try {
      toProcess.forEach {
        executeInNewTransaction(transactionManager) {
          println("Processing target $it")
          successfulTargets.add(it)
        }
      }
      execution.successTargets = successfulTargets.toList()
    } catch (e: Exception) {
      message.retries++
      if (message.retries > 3) {
        throw AmqpRejectAndDontRequeueException("Max retires exceeded", e)
      }
      rabbitTemplate.convertAndSend("batch-operations-wait-queue", convertMessage(message))
      Sentry.captureException(e)
    } finally {
      executeInNewTransaction(transactionManager) {
        execution.successTargets = successfulTargets.toList()
        entityManager.persist(execution)
      }
    }
  }
}
