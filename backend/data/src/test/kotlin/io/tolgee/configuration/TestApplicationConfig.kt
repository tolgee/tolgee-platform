package io.tolgee.configuration

import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import javax.sql.DataSource

/**
 * Custom test configuration that loads only the components needed for integration tests.
 * This configuration helps reduce the application context load time significantly.
 */
@Configuration
@Profile("test")
@AutoConfigureAfter(DataSourceAutoConfiguration::class)
class TestApplicationConfig {
    
    @Bean
    fun testEntityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = dataSource
        em.setPackagesToScan("io.tolgee.model")
        
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        
        val properties = HashMap<String, Any>()
        // Use create-drop for tests to ensure clean state
        properties["hibernate.hbm2ddl.auto"] = "create-drop"
        properties["hibernate.dialect"] = "org.hibernate.dialect.PostgreSQLDialect"
        properties["hibernate.show_sql"] = false
        properties["hibernate.format_sql"] = false
        
        // Performance optimizations
        properties["hibernate.jdbc.batch_size"] = 50
        properties["hibernate.order_inserts"] = true
        properties["hibernate.order_updates"] = true
        properties["hibernate.jdbc.batch_versioned_data"] = true
        
        // Disable second-level cache for tests
        properties["hibernate.cache.use_second_level_cache"] = false
        properties["hibernate.cache.use_query_cache"] = false
        
        // Optimize schema generation
        properties["hibernate.generate_statistics"] = false
        
        em.setJpaPropertyMap(properties)
        
        return em
    }
    
    /**
     * Configure minimal beans required for testing
     * This helps avoid loading unnecessary services and components
     */
    @Bean
    @Profile("test")
    fun testBeanPostProcessor(): TestBeanPostProcessor {
        return TestBeanPostProcessor()
    }
} 