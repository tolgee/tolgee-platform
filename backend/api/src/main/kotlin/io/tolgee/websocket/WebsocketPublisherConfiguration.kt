package io.tolgee.websocket

import io.tolgee.configuration.tolgee.WebsocketProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate

@Configuration
class WebsocketPublisherConfiguration(
  private val websocketProperties: WebsocketProperties,
  private val applicationContext: ApplicationContext,
) {
  @Bean
  fun websocketEventPublisher(): WebsocketEventPublisher {
    if (websocketProperties.useRedis) {
      return RedisWebsocketEventPublisher(applicationContext.getBean(StringRedisTemplate::class.java))
    }
    return SimpleWebsocketEventPublisher(applicationContext.getBean(SimpMessagingTemplate::class.java))
  }
}
