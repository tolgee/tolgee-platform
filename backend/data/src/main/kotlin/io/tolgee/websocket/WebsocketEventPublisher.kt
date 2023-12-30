package io.tolgee.websocket

interface WebsocketEventPublisher {
  operator fun invoke(
    destination: String,
    message: WebsocketEvent,
  )
}
