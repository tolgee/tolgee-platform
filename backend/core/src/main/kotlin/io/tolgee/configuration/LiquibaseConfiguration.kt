package io.tolgee.configuration

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.TolgeeProperties
import liquibase.integration.spring.SpringLiquibase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class LiquibaseConfiguration(
  val tolgeeProperties: TolgeeProperties,
) {
  @Bean
  @Primary
  fun liquibase(
    dataSource: DataSource,
    postgresRunner: PostgresRunner?,
  ): SpringLiquibase {
    val liquibase = SpringLiquibase()
    liquibase.setShouldRun(postgresRunner?.shouldRunMigrations != false)
    liquibase.dataSource = dataSource
    liquibase.changeLog = "classpath:db/changelog/schema.xml"
    liquibase.defaultSchema = "public"
    liquibase.isClearCheckSums = tolgeeProperties.internal.clearLiquibaseChecksums

    return liquibase
  }
}
