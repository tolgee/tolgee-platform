package io.tolgee.pubSub

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

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
  private val objectMapper: ObjectMapper,
) {
  companion object {
    const val WEBSOCKET_TOPIC = "websocket"
    const val JOB_QUEUE_TOPIC = "job_queue"
    const val JOB_CANCEL_TOPIC = "job_cancel"
  }

  @Bean
  fun redisPubsubReceiver(): RedisPubSubReceiver {
    return RedisPubSubReceiver(template, applicationEventPublisher, objectMapper)
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
  fun redisWebsocketPubsubContainer(): RedisMessageListenerContainer {
    val container = RedisMessageListenerContainer()
    container.setConnectionFactory(connectionFactory)
    if (tolgeeProperties.websocket.useRedis) {
      container.addMessageListener(redisWebsocketPubsubListenerAdapter(), PatternTopic(WEBSOCKET_TOPIC))
    }
    return container
  }

  /**
   * Job-queue and job-cancel messages mutate the in-memory [io.tolgee.batch.BatchJobChunkExecutionQueue]
   * and must be applied in the order they were published. A single consume cycle publishes a REMOVE
   * (consuming) immediately followed by an ADD (retry requeue) for the same chunk; if the ADD is applied
   * before the REMOVE, the REMOVE deletes the requeued chunk and it is silently dropped until the next
   * DB re-scan. RedisMessageListenerContainer's default executor is an unbounded SimpleAsyncTaskExecutor
   * that dispatches every message on a new thread, so order is not preserved. A single-threaded executor
   * forces sequential, in-order delivery.
   */
  @Bean
  fun redisJobPubsubContainer(): RedisMessageListenerContainer {
    val container = RedisMessageListenerContainer()
    container.setConnectionFactory(connectionFactory)
    container.setTaskExecutor(
      ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 1
        setThreadNamePrefix("redis-job-pubsub-")
        initialize()
      },
    )
    container.addMessageListener(redisJobQueuePubsubListenerAdapter(), PatternTopic(JOB_QUEUE_TOPIC))
    container.addMessageListener(redisJobCancelPubsubListenerAdapter(), PatternTopic(JOB_CANCEL_TOPIC))
    return container
  }
}
