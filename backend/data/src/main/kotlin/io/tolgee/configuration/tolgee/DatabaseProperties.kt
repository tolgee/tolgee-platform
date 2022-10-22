package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.database")
class DatabaseProperties {
  var type: DatabaseType = DatabaseType.POSTGRES

  enum class DatabaseType {
    COCKROACH, POSTGRES
  }
}
