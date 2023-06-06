package io.tolgee.rabbitMqRunners

import io.tolgee.configuration.tolgee.RabbitmqAutostartProperties
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class RabbitMqRunnerFactory(
  private val rabbitMqAutostartProperties: RabbitmqAutostartProperties,
  private val applicationContext: ApplicationContext
) {

  val runner: RabbitMqRunner by lazy {
    if (rabbitMqAutostartProperties.mode == RabbitmqAutostartProperties.RabbitMqAutostartMode.DOCKER) {
      return@lazy applicationContext.getBean(RabbitMqDockerRunner::class.java)
    }

    throw IllegalStateException("RabbitMq autostart mode: '${rabbitMqAutostartProperties.mode}' not recognized.")
  }
}
