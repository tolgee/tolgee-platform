package io.tolgee.component.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.BatchJobChunkMessage
import io.tolgee.service.batch.BatchJobService
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
@RabbitListener(queues = ["batch-operations"], concurrency = "1")
class BatchReceiver(
  private val batchJobService: BatchJobService
) {
  @RabbitHandler
  fun receive(stringMessage: String) {
    val message = jacksonObjectMapper().readValue<BatchJobChunkMessage>(stringMessage)
    batchJobService.processChunk(message)
  }
}
