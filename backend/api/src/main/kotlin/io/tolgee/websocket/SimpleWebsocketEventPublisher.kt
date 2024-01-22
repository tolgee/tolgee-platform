package io.tolgee.websocket

import org.springframework.messaging.simp.SimpMessagingTemplate

class SimpleWebsocketEventPublisher(
  private val websocketTemplate: SimpMessagingTemplate,
) : WebsocketEventPublisher {
  override operator fun invoke(
    destination: String,
    message: WebsocketEvent,
  ) {
    websocketTemplate.convertAndSend(destination, message)
  }
}
