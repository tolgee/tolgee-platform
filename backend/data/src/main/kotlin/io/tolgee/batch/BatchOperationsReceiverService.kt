package io.tolgee.batch

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.BATCH_OPERATIONS_AFTER_WAIT_QUEUE
import io.tolgee.configuration.BATCH_OPERATIONS_QUEUE
import io.tolgee.configuration.BATCH_OPERATIONS_WAIT_QUEUE

import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
@Lazy(false)
class BatchOperationsReceiverService(
  private val batchJobService: BatchJobService,
  private val currentDateProvider: CurrentDateProvider,
  private val rabbitTemplate: RabbitTemplate,
  private val applicationContext: ApplicationContext,
  private val chunkProcessingUtilFactory: ChunkProcessingUtilFactory
) {
  @RabbitListener(queues = [BATCH_OPERATIONS_QUEUE], concurrency = "1")
  fun receiveMessage(message: Message) {
    val factory = chunkProcessingUtilFactory.process(message, applicationContext)
    factory.processChunk()
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
