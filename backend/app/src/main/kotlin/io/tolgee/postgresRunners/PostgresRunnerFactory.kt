package io.tolgee.postgresRunners

import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class PostgresRunnerFactory(
  private val postgresAutostartProperties: PostgresAutostartProperties,
  private val applicationContext: ApplicationContext
) {

  val runner: PostgresRunner by lazy {
    if (postgresAutostartProperties.mode == PostgresAutostartProperties.PostgresAutostartMode.DOCKER) {
      return@lazy applicationContext.getBean(PostgresDockerRunner::class.java)
    }
    if (postgresAutostartProperties.mode == PostgresAutostartProperties.PostgresAutostartMode.EMBEDDED) {
      return@lazy applicationContext.getBean(PostgresEmbeddedRunner::class.java)
    }

    throw IllegalStateException("Postgres autostart mode: '${postgresAutostartProperties.mode}' not recognized.")
  }
}
