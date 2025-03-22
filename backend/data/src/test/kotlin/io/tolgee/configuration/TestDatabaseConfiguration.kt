package io.tolgee.configuration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import javax.sql.DataSource

@TestConfiguration
class TestDatabaseConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(name = ["test.database.type"], havingValue = "h2", matchIfMissing = true)
    fun h2DataSource(): DataSource {
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
            
            // Add properties to optimize H2 for testing
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        
        return HikariDataSource(config)
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = ["test.database.type"], havingValue = "embedded")
    fun embeddedDataSource(): DataSource {
        return EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .build()
    }
} 