package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.rabbitmq-autostart")
class RabbitmqAutostartProperties {
  var enabled: Boolean = true
  var mode: RabbitMqAutostartMode = RabbitMqAutostartMode.DOCKER
  var port: Int = 25672
  var managementPort: Int = 25673
  var containerName: String = "tolgee_rabbitmq"
  var defaultUser: String = "rabbit"
  var defaultPassword: String = "rabbit"

  enum class RabbitMqAutostartMode {
    /**
     * Starts docker container with postgres
     */
    DOCKER,

    /**
     * Expects that postgres is installed in the same container.
     * So the Postgres is started with Tolgee.
     */
    EMBEDDED,
  }
}
