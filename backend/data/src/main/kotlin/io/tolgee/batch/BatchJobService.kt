package io.tolgee.batch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.configuration.BATCH_OPERATIONS_QUEUE
import io.tolgee.dtos.BatchJobChunkMessageBody
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.repository.BatchJobRepository
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class BatchJobService(
  private val batchJobRepository: BatchJobRepository,
  private val entityManager: EntityManager,
  private val rabbitTemplate: RabbitTemplate,
  private val applicationContext: ApplicationContext,
) {
  @Transactional
  fun startJob(request: Any, author: UserAccount?, type: BatchJobType): BatchJob {
    val processor = getProcessor(type)
    val target = processor.getTarget(request)

    val chunked = target.chunked(BatchJobType.TRANSLATION.chunkSize)

    val job = createJob(author, target, chunked, BatchJobType.TRANSLATION.chunkSize)

    val params = processor.getParams(request, job)

    batchJobRepository.save(job)
    entityManager.persist(params)

    chunked.forEachIndexed { index, chunk ->
      val json = convertMessage(BatchJobChunkMessageBody(job.id, index))
      rabbitTemplate.convertAndSend("tolgee-exchange", BATCH_OPERATIONS_QUEUE, json)
    }

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

  fun convertMessage(message: BatchJobChunkMessageBody): String =
    jacksonObjectMapper().writeValueAsString(message)

  fun parseMessage(message: Message): BatchJobChunkMessageBody =
    jacksonObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .readValue(message.body)

  fun findJob(id: Long): BatchJob? {
    return batchJobRepository.findById(id).orElse(null)
  }

  fun getJob(id: Long): BatchJob {
    return findJob(id) ?: throw NotFoundException(io.tolgee.constants.Message.BATCH_JOB_NOT_FOUND)
  }

  fun getProcessor(type: BatchJobType): ChunkProcessor =
    applicationContext.getBean(type.processor.java)
}
