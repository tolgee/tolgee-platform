package io.tolgee.pubSub

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.batch.JobQueueItemEvent
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.websocket.RedisWebsocketEventWrapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate

class RedisPubSubReceiver(
  private val template: SimpMessagingTemplate,
  private val applicationEventPublisher: ApplicationEventPublisher
) : Logging {

  fun receiveWebsocketMessage(message: String) {
    val data = jacksonObjectMapper().readValue(message, RedisWebsocketEventWrapper::class.java)
    template.convertAndSend(data.destination, data.message)
    logger.debug("Sending message to ${data.destination}")
  }

  fun receiveJobQueueMessage(message: String) {
    val data = jacksonObjectMapper().readValue(message, JobQueueItemEvent::class.java)
    applicationEventPublisher.publishEvent(data)
  }
}
