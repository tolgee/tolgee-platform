package io.tolgee.config

import org.hibernate.cfg.AvailableSettings
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class HibernateOptimizationConfig {
    
    @Bean
    fun hibernatePropertiesCustomizer(): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer { hibernateProperties ->
            // Disable schema validation to speed up startup
            hibernateProperties[AvailableSettings.VALIDATE_SCHEMA] = false
            
            // Optimize query plans
            hibernateProperties[AvailableSettings.STATEMENT_BATCH_SIZE] = 50
            hibernateProperties[AvailableSettings.ORDER_INSERTS] = true
            hibernateProperties[AvailableSettings.ORDER_UPDATES] = true
            hibernateProperties[AvailableSettings.BATCH_VERSIONED_DATA] = true
            
            // Disable second-level cache for tests
            hibernateProperties[AvailableSettings.USE_SECOND_LEVEL_CACHE] = false
            hibernateProperties[AvailableSettings.USE_QUERY_CACHE] = false
            
            // Optimize connection handling
            hibernateProperties[AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT] = true
            
            // Optimize schema generation
            hibernateProperties[AvailableSettings.HBM2DDL_AUTO] = "create-drop"
            hibernateProperties[AvailableSettings.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR] = "org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor"
            
            // Disable unnecessary features
            hibernateProperties[AvailableSettings.GENERATE_STATISTICS] = false
            hibernateProperties[AvailableSettings.LOG_SESSION_METRICS] = false
        }
    }
} 