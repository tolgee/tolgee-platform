package io.tolgee.websocket

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.messaging.simp.SimpMessagingTemplate

@Configuration
@ConditionalOnProperty(name = ["tolgee.websocket.use-redis"], havingValue = "true")
class WebsocketRedisPubSubReceiverConfiguration(
  private val template: SimpMessagingTemplate,
  private val connectionFactory: RedisConnectionFactory
) {
  @Bean
  fun redisPubsubReceiver(): RedisPubSubReceiver {
    return RedisPubSubReceiver(template)
  }

  @Bean
  fun redisPubsubListenerAdapter(): MessageListenerAdapter {
    return MessageListenerAdapter(redisPubsubReceiver(), RedisPubSubReceiver::receiveMessage.name)
  }

  @Bean
  fun redisPubsubContainer(): RedisMessageListenerContainer {
    val container = RedisMessageListenerContainer()
    container.connectionFactory = connectionFactory
    container.addMessageListener(redisPubsubListenerAdapter(), PatternTopic("websocket"))
    return container
  }
}
