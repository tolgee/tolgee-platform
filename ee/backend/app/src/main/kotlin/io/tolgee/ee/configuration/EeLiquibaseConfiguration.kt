package io.tolgee.ee.configuration

import io.tolgee.PostgresRunner
import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories("io.tolgee.ee.repository")
@EntityScan(basePackages = ["io.tolgee.ee.model"])
class EeLiquibaseConfiguration {
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

    return liquibase
  }
}
