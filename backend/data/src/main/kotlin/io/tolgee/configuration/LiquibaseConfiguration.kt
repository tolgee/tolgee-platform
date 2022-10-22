package io.tolgee.configuration

import io.tolgee.configuration.tolgee.DatabaseProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import liquibase.integration.spring.SpringLiquibase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class LiquibaseConfiguration {
  @Bean
  @Primary
  fun liquibase(dataSource: DataSource, properties: TolgeeProperties): SpringLiquibase {
    val liquibase = SpringLiquibase()

    liquibase.dataSource = dataSource
    liquibase.changeLog = "classpath:db/changelog/schema.xml"
    liquibase.defaultSchema = "public"
    if (properties.database.type == DatabaseProperties.DatabaseType.COCKROACH) {
      liquibase.setChangeLogParameters(mapOf("isCockroach" to "true"))
    }
    return liquibase
  }
}
