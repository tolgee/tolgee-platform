package io.tolgee.config

import io.tolgee.testing.ContextLoadTimeListener
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class TestApplicationRunner : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        // Start tracking context load time
        ContextLoadTimeListener.startTracking()
    }
} 