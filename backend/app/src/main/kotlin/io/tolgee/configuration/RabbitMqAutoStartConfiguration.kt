package io.tolgee.configuration

import io.tolgee.configuration.tolgee.RabbitmqAutostartProperties
import io.tolgee.rabbitMqRunners.RabbitMqRunner
import io.tolgee.rabbitMqRunners.RabbitMqRunnerFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnProperty(name = ["tolgee.rabbitmq-autostart.enabled"], havingValue = "true")
class RabbitMqAutoStartConfiguration(
  val rabbitmqAutostartProperties: RabbitmqAutostartProperties,
  val rabbitMqRunnerFactory: RabbitMqRunnerFactory
) {

  @Bean
  @Primary
  fun connectionFactory(): ConnectionFactory {
    rabbitMqRunner.run()
    val connectionFactory = CachingConnectionFactory()
    connectionFactory.host = "localhost"
    connectionFactory.port = rabbitmqAutostartProperties.port
    connectionFactory.username = rabbitmqAutostartProperties.defaultUser
    connectionFactory.setPassword(rabbitmqAutostartProperties.defaultPassword)
    connectionFactory.cacheMode = CachingConnectionFactory.CacheMode.CHANNEL
    return connectionFactory
  }

  private val rabbitMqRunner: RabbitMqRunner by lazy { rabbitMqRunnerFactory.runner }
}
