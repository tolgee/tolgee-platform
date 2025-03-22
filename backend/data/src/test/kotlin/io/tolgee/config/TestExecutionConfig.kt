package io.tolgee.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@TestConfiguration
@Profile("test")
class TestExecutionConfig {
    
    @Bean
    fun testTaskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = Runtime.getRuntime().availableProcessors()
        executor.maxPoolSize = Runtime.getRuntime().availableProcessors() * 2
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("test-executor-")
        executor.initialize()
        return executor
    }
} 