package io.tolgee.configuration

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfiguration {

  @Bean
  fun queue(): Queue {
    return QueueBuilder
      .durable("batch-operations")
      .deadLetterExchange("dead-letter-exchange")
      .deadLetterRoutingKey("dead-letter")
      .quorum()
      .deliveryLimit(5)
      .build()
  }

  @Bean
  fun deadLetterQue(): Queue {
    return QueueBuilder
      .durable("dead-letter-queue")
      .build()
  }

  @Bean
  fun waitQueue(): Queue {
    return QueueBuilder
      .durable("batch-operations-wait-queue")
      .deadLetterExchange("tolgee-exchange")
      .deadLetterRoutingKey("batch-operations")
      .ttl(1000 * 10)
      .build()
  }

  @Bean
  fun exchange(): TopicExchange {
    return TopicExchange("tolgee-exchange")
  }

  @Bean
  fun deadLetterExchange(): DirectExchange {
    return DirectExchange("dead-letter-exchange")
  }

  @Bean
  fun dlqBinding(): Binding {
    return BindingBuilder.bind(deadLetterQue()).to(deadLetterExchange()).with("dead-letter")
  }

  @Bean
  fun tolgeeExchangeBinding(): Binding {
    return BindingBuilder.bind(queue()).to(exchange()).with("batch-operations")
  }
}
