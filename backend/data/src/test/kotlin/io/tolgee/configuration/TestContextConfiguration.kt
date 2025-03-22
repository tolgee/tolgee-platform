package io.tolgee.configuration

import io.tolgee.testing.DataIsolationTestExecutionListener
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import javax.sql.DataSource
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig

@TestConfiguration
@EnableAutoConfiguration
@TestExecutionListeners(
    listeners = [
        DependencyInjectionTestExecutionListener::class,
        DataIsolationTestExecutionListener::class,
        TransactionalTestExecutionListener::class
    ]
)
class TestContextConfiguration {

    @Bean
    @Primary
    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            driverClassName = "org.h2.Driver"
            jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
            username = "sa"
            password = ""
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 10000
            connectionTimeout = 5000
            leakDetectionThreshold = 10000
            connectionTestQuery = "SELECT 1"
            
            // Add properties to optimize H2 for testing
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        
        return HikariDataSource(config)
    }
} 