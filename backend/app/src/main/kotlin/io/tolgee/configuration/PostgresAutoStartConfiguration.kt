package io.tolgee.configuration

import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import io.tolgee.postgresRunners.PostgresRunner
import io.tolgee.postgresRunners.PostgresRunnerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(name = ["tolgee.postgres-autostart.enabled"], havingValue = "true")
class PostgresAutoStartConfiguration(
  val postgresAutostartProperties: PostgresAutostartProperties,
  val postgresRunnerFactory: PostgresRunnerFactory
) {

  private var _dataSource: DataSource? = null

  @Bean
  fun getDataSource(): DataSource {
    _dataSource?.let { return it }
    postgresRunner.run()
    _dataSource = buildDataSource()
    return _dataSource!!
  }

  private fun buildDataSource(): DataSource {
    val dataSourceBuilder = DataSourceBuilder.create()
    dataSourceBuilder.url(postgresRunner.datasourceUrl)
    dataSourceBuilder.username(postgresAutostartProperties.user)
    dataSourceBuilder.password(postgresAutostartProperties.password)
    return dataSourceBuilder.build()
  }

  private val postgresRunner: PostgresRunner
    get() = postgresRunnerFactory.runner
}
