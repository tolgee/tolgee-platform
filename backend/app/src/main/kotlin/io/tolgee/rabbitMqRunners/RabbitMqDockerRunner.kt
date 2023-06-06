package io.tolgee.rabbitMqRunners

import io.tolgee.configuration.tolgee.RabbitmqAutostartProperties
import io.tolgee.misc.dockerRunner.DockerContainerRunner
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import javax.annotation.PreDestroy

@Component
@Scope(SCOPE_SINGLETON)
class RabbitMqDockerRunner(
  protected val rabbitMqAutostartProperties: RabbitmqAutostartProperties,
) : RabbitMqRunner {
  private var instance: DockerContainerRunner? = null
  private val logger = LoggerFactory.getLogger(javaClass)

  override fun run() {
    instance = DockerContainerRunner(
      image = "rabbitmq:3.11-management-alpine",
      expose = mapOf(
        rabbitMqAutostartProperties.port.toString() to "5672",
        rabbitMqAutostartProperties.managementPort.toString() to "15672"
      ),
      waitForLog = "Server startup complete;",
      waitForLogTimesForNewContainer = 1,
      waitForLogTimesForExistingContainer = 1,
      rm = false,
      name = rabbitMqAutostartProperties.containerName,
      stopBeforeStart = false,
      env = mapOf(
        "RABBITMQ_DEFAULT_USER" to rabbitMqAutostartProperties.defaultUser,
        "RABBITMQ_DEFAULT_PASS" to rabbitMqAutostartProperties.defaultPassword,
      ),
      timeout = 300000,
    ).also {
      logger.info("Starting RabbitMQ container")
      it.run()
    }
  }

  @PreDestroy
  fun preDestroy() {
    instance?.let {
      logger.info("Stopping RabbitMQ container")
      it.stop()
    }
  }
}
