package io.tolgee.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["io.tolgee.repository"])
@EnableTransactionManagement
class RepositoryConfiguration {
    // This configuration enables optimized repository queries
    // and transaction management for better performance
} 