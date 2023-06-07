package io.tolgee.configuration

import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BatchOperationsQueueConfiguration {
  companion object {
    const val BATCH_OPERATIONS_WAIT_QUEUE = "batch-operations-wait-queue"
    const val BATCH_OPERATIONS_AFTER_WAIT_QUEUE = "batch-operations-after-wait-queue"
    const val BATCH_OPERATIONS_QUEUE = "batch-operations"
    const val BATCH_OPERATIONS_FAILED_CHUNKS_QUEUE = "batch-operations-failed-chunks-queue"
  }

  init {
    println("initted config")
  }

  @Bean
  fun batchOperationsQueue(): Queue {
    return QueueBuilder
      .durable(BATCH_OPERATIONS_QUEUE)
      .deadLetterExchange(exchange().name)
      .deadLetterRoutingKey(BATCH_OPERATIONS_FAILED_CHUNKS_QUEUE)
      .quorum()
      .build()
  }

  @Bean
  fun failedChunksQueue(): Queue {
    return QueueBuilder
      .durable(BATCH_OPERATIONS_FAILED_CHUNKS_QUEUE)
      .quorum()
      .build()
  }

  @Bean
  fun waitQueue(): Queue {
    return QueueBuilder
      .durable(BATCH_OPERATIONS_WAIT_QUEUE)
      .deadLetterExchange(exchange().name)
      .deadLetterRoutingKey(BATCH_OPERATIONS_AFTER_WAIT_QUEUE)
      .quorum()
      .ttl(500)
      .build()
  }

  @Bean
  fun afterWaitQueue(): Queue {
    return QueueBuilder
      .durable(BATCH_OPERATIONS_AFTER_WAIT_QUEUE)
      .quorum()
      .build()
  }

  @Bean
  fun exchange(): DirectExchange {
    return DirectExchange("tolgee-exchange")
  }

  @Bean
  fun binding(): Binding {
    return BindingBuilder
      .bind(batchOperationsQueue())
      .to(exchange())
      .with(BATCH_OPERATIONS_QUEUE)
  }

  @Bean
  fun amqpAdmin(connectionFactory: ConnectionFactory): AmqpAdmin {
    return RabbitAdmin(connectionFactory)
  }
}
