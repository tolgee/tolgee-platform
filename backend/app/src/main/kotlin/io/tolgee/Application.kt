package io.tolgee

import io.tolgee.configuration.Banner
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(
  scanBasePackages = ["io.tolgee"],
  exclude = [LdapAutoConfiguration::class],
)
@EnableJpaAuditing
@EntityScan("io.tolgee.model")
@EnableConfigurationProperties(TolgeeProperties::class)
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
