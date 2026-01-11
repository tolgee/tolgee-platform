package io.tolgee.websocket

class RedisWebsocketEventWrapper<T>(
  val destination: String,
  val message: T,
)
