package io.tolgee.pubSub

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.messaging.simp.SimpMessagingTemplate

@Configuration
@ConditionalOnExpression(
  "\${tolgee.websocket.use-redis:false} or " +
    "(\${tolgee.cache.use-redis:false} and \${tolgee.cache.enabled:false})",
)
class RedisPubSubReceiverConfiguration(
  private val template: SimpMessagingTemplate,
  private val connectionFactory: RedisConnectionFactory,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val tolgeeProperties: TolgeeProperties,
) {
  companion object {
    const val WEBSOCKET_TOPIC = "websocket"
    const val JOB_QUEUE_TOPIC = "job_queue"
    const val JOB_CANCEL_TOPIC = "job_cancel"
  }

  @Bean
  fun redisPubsubReceiver(): RedisPubSubReceiver {
    return RedisPubSubReceiver(template, applicationEventPublisher)
  }

  @Bean
  fun redisWebsocketPubsubListenerAdapter(): MessageListenerAdapter {
    return MessageListenerAdapter(redisPubsubReceiver(), RedisPubSubReceiver::receiveWebsocketMessage.name)
  }

  @Bean
  fun redisJobQueuePubsubListenerAdapter(): MessageListenerAdapter {
    return MessageListenerAdapter(redisPubsubReceiver(), RedisPubSubReceiver::receiveJobQueueMessage.name)
  }

  @Bean
  fun redisJobCancelPubsubListenerAdapter(): MessageListenerAdapter {
    return MessageListenerAdapter(redisPubsubReceiver(), RedisPubSubReceiver::receiveJobCancel.name)
  }

  @Bean
  fun redisPubsubContainer(): RedisMessageListenerContainer {
    val container = RedisMessageListenerContainer()
    container.setConnectionFactory(connectionFactory)
    if (tolgeeProperties.websocket.useRedis) {
      container.addMessageListener(redisWebsocketPubsubListenerAdapter(), PatternTopic(WEBSOCKET_TOPIC))
    }
    container.addMessageListener(redisJobQueuePubsubListenerAdapter(), PatternTopic(JOB_QUEUE_TOPIC))
    container.addMessageListener(redisJobCancelPubsubListenerAdapter(), PatternTopic(JOB_CANCEL_TOPIC))
    return container
  }
}
