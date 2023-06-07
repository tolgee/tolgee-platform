package io.tolgee.configuration

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Configuration
class BatchOperationsQueueConfiguration {
  @Bean
  @Lazy(false)
  fun batchOperationsQueue(): Queue {
    return QueueBuilder
      .durable(BATCH_OPERATIONS_QUEUE)
      .deadLetterExchange(exchange().name)
      .deadLetterRoutingKey(BATCH_OPERATIONS_FAILED_CHUNKS_QUEUE)
      .quorum()
      .build()
  }

  @Bean
  @Lazy(false)
  fun failedChunksQueue(): Queue {
    return QueueBuilder
      .durable(BATCH_OPERATIONS_FAILED_CHUNKS_QUEUE)
      .quorum()
      .build()
  }

  @Bean
  @Lazy(false)
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
  @Lazy(false)
  fun afterWaitQueue(): Queue {
    return QueueBuilder
      .durable(BATCH_OPERATIONS_AFTER_WAIT_QUEUE)
      .quorum()
      .build()
  }

  @Bean
  @Lazy(false)
  fun exchange(): DirectExchange {
    return DirectExchange("tolgee-exchange")
  }

  @Bean
  @Lazy(false)
  fun binding(): Binding {
    return BindingBuilder
      .bind(batchOperationsQueue())
      .to(exchange())
      .with(BATCH_OPERATIONS_QUEUE)
  }

  @Bean
  @Lazy(false)
  fun amqpAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
    return RabbitAdmin(connectionFactory)
  }

  @Bean
  @Lazy(false)
  fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
    return RabbitTemplate(connectionFactory)
  }
}
