package io.tolgee

import io.tolgee.configuration.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.endpoint.jmx.JmxEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.liquibase.LiquibaseEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.jdbc.DataSourcePoolMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.startup.StartupTimeMetricsListenerAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat.TomcatMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
  exclude = [
    // RedissonAutoConfiguration::class,
    CompositeMeterRegistryAutoConfiguration::class,
    DataSourcePoolMetricsAutoConfiguration::class,
    DiskSpaceHealthContributorAutoConfiguration::class,
    InfoContributorAutoConfiguration::class,
    JmxAutoConfiguration::class,
    JvmMetricsAutoConfiguration::class,
    JmxEndpointAutoConfiguration::class,
    LdapAutoConfiguration::class,
    LiquibaseEndpointAutoConfiguration::class,
    MetricsEndpointAutoConfiguration::class,
    StartupTimeMetricsListenerAutoConfiguration::class,
    TomcatMetricsAutoConfiguration::class,
  ],
  scanBasePackages = ["io.tolgee"]
)
@EnableJpaAuditing
@EntityScan("io.tolgee.model")
@ConfigurationPropertiesScan
@EnableJpaRepositories("io.tolgee.repository")
class Application {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val app = SpringApplication(Application::class.java)
      app.setBanner(Banner())
      app.run(*args)
    }
  }
}
