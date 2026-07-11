package io.tolgee.pubSub

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.events.JobCancelEvent
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.websocket.RedisWebsocketEventWrapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate

class RedisPubSubReceiver(
  private val template: SimpMessagingTemplate,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val objectMapper: ObjectMapper,
) : Logging {
  fun receiveWebsocketMessage(message: String) {
    val data = objectMapper.readValue(message, RedisWebsocketEventWrapper::class.java)
    data.message?.let {
      template.convertAndSend(data.destination, it)
      logger.debug("Sending message to ${data.destination}")
    }
  }

  fun receiveJobQueueMessage(message: String) {
    val data = objectMapper.readValue(message, JobQueueItemsEvent::class.java)
    applicationEventPublisher.publishEvent(data)
  }

  fun receiveJobCancel(message: String) {
    val data = objectMapper.readValue(message, Long::class.java)
    applicationEventPublisher.publishEvent(JobCancelEvent(data))
  }
}
