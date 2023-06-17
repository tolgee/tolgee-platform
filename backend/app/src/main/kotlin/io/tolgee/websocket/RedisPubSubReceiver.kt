package io.tolgee.websocket

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.messaging.simp.SimpMessagingTemplate

class RedisPubSubReceiver(
  private val template: SimpMessagingTemplate
) : Logging {

  fun receiveMessage(message: String) {
    val data = jacksonObjectMapper().readValue(message, RedisWebsocketEventWrapper::class.java)
    template.convertAndSend(data.destination, data.message)
    logger.debug("Sending message to ${data.destination}")
  }
}
