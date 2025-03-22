package io.tolgee.config

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
@Profile("test")
class TestDatabaseInitializer {
    
    @Bean
    fun databaseInitializer(dataSource: DataSource): CommandLineRunner {
        return CommandLineRunner {
            val jdbcTemplate = JdbcTemplate(dataSource)
            
            // Optimize H2 database for testing
            jdbcTemplate.execute("SET MODE PostgreSQL")
            jdbcTemplate.execute("SET DB_CLOSE_DELAY -1")
            jdbcTemplate.execute("SET LOCK_MODE 0")
            jdbcTemplate.execute("SET LOCK_TIMEOUT 10000")
            jdbcTemplate.execute("SET CACHE_SIZE 65536")
            jdbcTemplate.execute("SET COMPRESS_LOB LZF")
            jdbcTemplate.execute("SET LOG 0")
            jdbcTemplate.execute("SET TRACE_LEVEL_FILE 0")
            jdbcTemplate.execute("SET TRACE_LEVEL_SYSTEM_OUT 0")
            jdbcTemplate.execute("SET WRITE_DELAY 0")
            
            // Disable referential integrity checks for faster inserts/updates
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE")
        }
    }
} 