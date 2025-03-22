package io.tolgee.configuration

import org.hibernate.cfg.AvailableSettings
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class TestHibernateConfiguration {
    
    @Bean
    fun hibernatePropertiesCustomizer(): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer { hibernateProperties ->
            // Disable schema validation to speed up startup
            hibernateProperties[AvailableSettings.VALIDATE_SCHEMA] = false
            
            // Use create-drop for faster schema creation
            hibernateProperties[AvailableSettings.HBM2DDL_AUTO] = "create-drop"
            
            // Disable SQL logging
            hibernateProperties[AvailableSettings.SHOW_SQL] = false
            hibernateProperties[AvailableSettings.FORMAT_SQL] = false
            
            // Optimize batch operations
            hibernateProperties[AvailableSettings.STATEMENT_BATCH_SIZE] = 50
            hibernateProperties[AvailableSettings.ORDER_INSERTS] = true
            hibernateProperties[AvailableSettings.ORDER_UPDATES] = true
            hibernateProperties[AvailableSettings.BATCH_VERSIONED_DATA] = true
            
            // Disable second-level cache for tests
            hibernateProperties[AvailableSettings.USE_SECOND_LEVEL_CACHE] = false
            hibernateProperties[AvailableSettings.USE_QUERY_CACHE] = false
            
            // Optimize connection acquisition
            hibernateProperties[AvailableSettings.ACQUIRE_CONNECTIONS_AGGRESSIVELY] = true
            
            // Disable statistics collection
            hibernateProperties[AvailableSettings.GENERATE_STATISTICS] = false
            
            // Use in-memory database dialect
            hibernateProperties[AvailableSettings.DIALECT] = "org.hibernate.dialect.H2Dialect"
        }
    }
} 