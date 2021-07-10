package io.tolgee

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.DbPopulatorReal
import io.tolgee.dtos.request.SignUpDto
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.UserAccountService
import org.redisson.spring.starter.RedissonAutoConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication(exclude = [RedissonAutoConfiguration::class])
@EnableJpaAuditing
@ConfigurationPropertiesScan
class Application(
        populator: DbPopulatorReal,
        userAccountService: UserAccountService,
        properties: TolgeeProperties,
        initialPasswordManager: InitialPasswordManager
) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }

    init {
        if (properties.internal.populate) {
            populator.autoPopulate()
        }

        val initialUsername = properties.authentication.initialUsername
        if (properties.authentication.createInitialUser && !userAccountService.isAnyUserAccount &&
                userAccountService.getByUserName(initialUsername).isEmpty
        ) {
            val initialPassword = initialPasswordManager.initialPassword

            userAccountService.createUser(
                    SignUpDto(email = initialUsername, password = initialPassword, name = initialUsername)
            )
        }
    }
}
