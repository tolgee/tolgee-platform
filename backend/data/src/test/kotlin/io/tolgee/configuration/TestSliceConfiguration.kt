package io.tolgee.config

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableAutoConfiguration
@ComponentScan(
    basePackages = ["io.tolgee"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ["io\\.tolgee\\.api\\..*", "io\\.tolgee\\.security\\..*"]
        )
    ]
)
@EntityScan("io.tolgee.model")
@EnableJpaRepositories("io.tolgee.repository")
@EnableTransactionManagement
@AutoConfigureDataJpa
class TestSliceConfiguration 