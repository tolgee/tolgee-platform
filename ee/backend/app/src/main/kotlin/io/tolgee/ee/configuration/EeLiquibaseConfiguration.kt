package io.tolgee.ee.configuration

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.TolgeeProperties
import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories("io.tolgee.ee.repository")
@EntityScan(basePackages = ["io.tolgee.ee.model"])
class EeLiquibaseConfiguration(
  val tolgeeProperties: TolgeeProperties,
) {
  @Bean("ee-liquibase")
  fun liquibase(
    dataSource: DataSource,
    postgresRunner: PostgresRunner?,
  ): SpringLiquibase {
    val liquibase = SpringLiquibase()

    liquibase.setShouldRun(postgresRunner?.shouldRunMigrations != false)
    liquibase.dataSource = dataSource
    liquibase.changeLog = "classpath:db/changelog/ee-schema.xml"
    liquibase.defaultSchema = "ee"
    liquibase.liquibaseSchema = "public"
    liquibase.isClearCheckSums = tolgeeProperties.internal.clearLiquibaseChecksums

    return liquibase
  }
}
