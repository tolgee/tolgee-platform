package io.tolgee.configuration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

/**
 * Database configuration specifically optimized for tests.
 * This configuration provides an optimized database connection pool
 * to reduce test execution time.
 */
@TestConfiguration
class TestDatabaseConfig {
    
    /**
     * Creates an optimized data source for tests.
     * Uses HikariCP with settings tuned for test performance.
     */
    @Bean
    @Primary
    fun dataSource(): DataSource {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        config.username = "sa"
        config.password = ""
        config.driverClassName = "org.h2.Driver"
        
        // Optimize connection pool for tests
        config.maximumPoolSize = 10
        config.minimumIdle = 5
        config.connectionTimeout = 1000 // 1 second
        config.idleTimeout = 60000 // 1 minute
        config.maxLifetime = 300000 // 5 minutes
        
        // Disable connection testing for tests
        config.connectionTestQuery = null
        config.validationTimeout = 250 // 250ms
        
        return HikariDataSource(config)
    }
    
    /**
     * Alternative: In-memory H2 database for even faster tests
     * Uncomment this and comment out the PostgreSQL dataSource above to use H2
     */
    /*
    @Bean
    @Primary
    fun inMemoryDataSource(): DataSource {
        val config = HikariConfig()
        
        // Use H2 in-memory database
        config.jdbcUrl = "jdbc:h2:mem:tolgee_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        config.driverClassName = "org.h2.Driver"
        config.username = "sa"
        config.password = ""
        
        // Connection pool optimization
        config.maximumPoolSize = 5
        config.minimumIdle = 1
        
        return HikariDataSource(config)
    }
    */
} 