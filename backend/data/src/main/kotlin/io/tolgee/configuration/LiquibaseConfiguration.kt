package io.tolgee.configuration

import liquibase.integration.spring.SpringLiquibase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class LiquibaseConfiguration {
  @Bean
  @Primary
  fun liquibase(dataSource: DataSource): SpringLiquibase {
    val liquibase = SpringLiquibase()

    liquibase.dataSource = dataSource
    liquibase.changeLog = "classpath:db/changelog/schema.xml"

    return liquibase
  }
}
