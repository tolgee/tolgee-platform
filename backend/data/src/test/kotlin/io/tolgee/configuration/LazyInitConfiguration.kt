package io.tolgee.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import java.time.Duration
import java.time.Instant

@Configuration
class LazyInitConfiguration {

    private var startTime: Instant? = null

    @EventListener(ContextRefreshedEvent::class)
    fun onContextRefreshed() {
        val end = Instant.now()
        startTime?.let {
            val duration = Duration.between(it, end)
            println("Application context initialized in ${duration.toMillis()} ms")
        }
    }

    @Bean
    @ConditionalOnProperty(name = ["spring.main.lazy-initialization"], havingValue = "true")
    fun contextStartTimeTracker(): Any {
        startTime = Instant.now()
        return Object()
    }
} 