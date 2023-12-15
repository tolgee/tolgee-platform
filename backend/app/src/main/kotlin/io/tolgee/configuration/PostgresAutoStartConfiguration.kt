package io.tolgee.configuration

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(name = ["tolgee.postgres-autostart.enabled"], havingValue = "true")
class PostgresAutoStartConfiguration(
  val postgresAutostartProperties: PostgresAutostartProperties,
) {
  private var _dataSource: DataSource? = null

  @Bean("dataSource")
  @ConfigurationProperties(prefix = "spring.datasource")
  fun getDataSource(postgresRunner: PostgresRunner?): DataSource {
    postgresRunner ?: throw IllegalStateException("Postgres runner is not initialized")
    _dataSource?.let { return it }
    postgresRunner.run()
    _dataSource = buildDataSource(postgresRunner)
    return _dataSource!!
  }

  private fun buildDataSource(postgresRunner: PostgresRunner): DataSource {
    val dataSourceBuilder = DataSourceBuilder.create()
    dataSourceBuilder.url(postgresRunner.datasourceUrl)
    dataSourceBuilder.username(postgresAutostartProperties.user)
    dataSourceBuilder.password(postgresAutostartProperties.password)
    return dataSourceBuilder.build()
  }
}
