package io.polygloat

import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.development.DbPopulatorReal
import io.polygloat.dtos.request.SignUpDto
import io.polygloat.security.InitialPasswordManager
import io.polygloat.service.UserAccountService
import io.sentry.Sentry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
open class Application(
        populator: DbPopulatorReal,
        userAccountService: UserAccountService,
        val properties: PolygloatProperties,
        val initialPasswordManager: InitialPasswordManager
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

        if (properties.sentry.enabled) {
            Sentry.init {
                it.dsn = properties.sentry.serverDsn
            }
        }

        val initialUsername = properties.authentication.initialUsername
        if (properties.authentication.createInitialUser && !userAccountService.isAnyUserAccount &&
                userAccountService.getByUserName(initialUsername).isEmpty) {
            val initialPassword = initialPasswordManager.initialPassword

            userAccountService.createUser(
                    SignUpDto.builder()
                            .email(initialUsername)
                            .password(initialPassword)
                            .name(initialUsername)
                            .build())
        }
    }
}