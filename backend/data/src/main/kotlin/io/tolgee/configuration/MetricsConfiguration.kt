package io.tolgee.configuration

import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class MetricsConfiguration(
  private val dataSource: DataSource
) {
  @Bean
  fun meterRegistry(): MeterRegistry {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    (dataSource as? HikariDataSource)?.metricRegistry = registry
    return registry
  }
}
