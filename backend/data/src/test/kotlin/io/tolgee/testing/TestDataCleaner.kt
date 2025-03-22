package io.tolgee.testing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

@Component
class TestDataCleaner {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate
    
    // Track which tables have been cleaned in this test run
    private val cleanedTables = ConcurrentHashMap<String, Boolean>()
    
    /**
     * Clean all tables only if they haven't been cleaned in this test run
     */
    @Transactional
    fun cleanAllTables() {
        if (cleanedTables.isEmpty()) {
            jdbcTemplate.execute("SET CONSTRAINTS ALL DEFERRED")
            
            // Get all table names
            val tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String::class.java
            )
            
            // Truncate all tables
            tables.forEach { table ->
                jdbcTemplate.execute("TRUNCATE TABLE $table CASCADE")
                cleanedTables[table] = true
            }
            
            jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE")
        }
    }
    
    /**
     * Clean specific tables only if they haven't been cleaned in this test run
     */
    @Transactional
    fun cleanSpecificTables(vararg tables: String) {
        jdbcTemplate.execute("SET CONSTRAINTS ALL DEFERRED")
        
        tables.forEach { table ->
            jdbcTemplate.execute("TRUNCATE TABLE $table CASCADE")
            cleanedTables[table] = true
        }
        
        jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE")
    }
    
    // Reset the cleaned tables tracking - call this between test classes
    fun reset() {
        cleanedTables.clear()
    }
} 