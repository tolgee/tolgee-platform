package io.tolgee.websocket

import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper

class RedisWebsocketEventPublisher(
  private val redisTemplate: StringRedisTemplate,
) : WebsocketEventPublisher {
  override operator fun invoke(
    destination: String,
    message: WebsocketEvent,
  ) {
    val messageString = jacksonObjectMapper().writeValueAsString(RedisWebsocketEventWrapper(destination, message))
    redisTemplate.convertAndSend(
      "websocket",
      messageString,
    )
  }
}
