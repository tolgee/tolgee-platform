package io.tolgee.websocket

fun interface WebsocketEventPublisher {
  operator fun invoke(
    destination: String,
    message: WebsocketEvent,
  )
}
