package io.tolgee.batch

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.BatchOperationsQueueConfiguration.Companion.BATCH_OPERATIONS_AFTER_WAIT_QUEUE
import io.tolgee.configuration.BatchOperationsQueueConfiguration.Companion.BATCH_OPERATIONS_QUEUE
import io.tolgee.configuration.BatchOperationsQueueConfiguration.Companion.BATCH_OPERATIONS_WAIT_QUEUE
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BatchOperationsReceiverService(
  private val batchJobService: BatchJobService,
  private val currentDateProvider: CurrentDateProvider,
  private val rabbitTemplate: RabbitTemplate,
  private val applicationContext: ApplicationContext,
  private val chunkProcessingUtilFactory: ChunkProcessingUtil.Factory
) {
  @RabbitListener(queues = [BATCH_OPERATIONS_QUEUE], concurrency = "1")
  @Transactional
  fun receiveMessage(message: Message) {
    chunkProcessingUtilFactory(message, applicationContext).processChunk()
  }

  @RabbitListener(queues = [BATCH_OPERATIONS_AFTER_WAIT_QUEUE], concurrency = "100")
  fun rePublish(message: Message) {
    val batchJobChunkMessage = batchJobService.parseMessage(message)

    if (batchJobChunkMessage.waitUntil != null && batchJobChunkMessage.waitUntil!! > currentDateProvider.date.time) {
      this.rabbitTemplate.send(BATCH_OPERATIONS_WAIT_QUEUE, message)
      return
    }

    this.rabbitTemplate.send(BATCH_OPERATIONS_QUEUE, message)
  }
}
