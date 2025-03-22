package io.tolgee.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

@TestConfiguration
@Profile("test")
class OptimizedTestConfiguration {

    @Bean
    @Primary
    fun optimizedJdbcTemplate(dataSource: DataSource): JdbcTemplate {
        val jdbcTemplate = JdbcTemplate(dataSource)
        jdbcTemplate.fetchSize = 100
        jdbcTemplate.queryTimeout = 5
        return jdbcTemplate
    }

    @Bean
    @Lazy
    fun testExecutor(): ThreadPoolExecutor {
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() / 2
        ) as ThreadPoolExecutor
    }

    @Bean
    @ConditionalOnProperty(name = ["test.performance.tracking.enabled"], havingValue = "true")
    fun performanceTracker(): PerformanceTracker {
        return PerformanceTracker()
    }

    class PerformanceTracker {
        private val startTimes = mutableMapOf<String, Long>()
        
        fun start(operation: String) {
            startTimes[operation] = System.currentTimeMillis()
        }
        
        fun end(operation: String): Long {
            val startTime = startTimes[operation] ?: return -1
            val duration = System.currentTimeMillis() - startTime
            println("Operation '$operation' took $duration ms")
            return duration
        }
    }
} 