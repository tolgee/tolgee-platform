package io.tolgee.websocket

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate

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
