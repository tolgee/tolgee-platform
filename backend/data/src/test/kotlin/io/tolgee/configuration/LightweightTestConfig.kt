package io.tolgee.configuration

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableAutoConfiguration
@ComponentScan(
    basePackages = ["io.tolgee"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = [
                "io\\.tolgee\\.configuration\\.tolgee\\..*",
                "io\\.tolgee\\.component\\.machineTranslation\\..*",
                "io\\.tolgee\\.component\\.contentDelivery\\..*"
            ]
        )
    ]
)
@EnableJpaRepositories(basePackages = ["io.tolgee.repository"])
@EntityScan(basePackages = ["io.tolgee.model"])
@Import(HibernateConfiguration::class, RepositoryConfiguration::class)
class LightweightTestConfig {
    // This configuration provides a minimal context for tests
    // that don't need the full application context
} 