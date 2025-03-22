package io.tolgee.configuration

import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.orm.jpa.JpaVendorAdapter
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import javax.sql.DataSource
import java.util.Properties

@Configuration
class HibernateConfiguration(
    private val jpaProperties: JpaProperties,
    private val hibernateProperties: HibernateProperties
) {
    @Bean
    fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = dataSource
        em.setPackagesToScan("io.tolgee.model")
        
        val vendorAdapter: JpaVendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        
        val properties = getHibernateProperties()
        em.setJpaProperties(properties)
        
        return em
    }
    
    private fun getHibernateProperties(): Properties {
        val properties = Properties()
        
        // Performance optimizations
        properties["hibernate.jdbc.batch_size"] = 50
        properties["hibernate.order_inserts"] = true
        properties["hibernate.order_updates"] = true
        properties["hibernate.jdbc.batch_versioned_data"] = true
        
        // Query optimizations
        properties["hibernate.query.fail_on_pagination_over_collection_fetch"] = true
        properties["hibernate.query.in_clause_parameter_padding"] = true
        
        // Connection pool optimizations
        properties["hibernate.connection.provider_class"] = "org.hibernate.hikaricp.internal.HikariCPConnectionProvider"
        properties["hibernate.hikari.minimumIdle"] = 5
        properties["hibernate.hikari.maximumPoolSize"] = 10
        properties["hibernate.hikari.idleTimeout"] = 30000
        
        // Add all properties from application configuration
        properties.putAll(hibernateProperties.determineHibernateProperties(
            jpaProperties.properties, 
            HibernateSettings()
        ))
        
        return properties
    }
    
    @Bean
    @Profile("test")
    fun testHibernateProperties(): Properties {
        val properties = Properties()
        
        // Optimize for tests
        properties["hibernate.jdbc.batch_size"] = 20
        properties["hibernate.order_inserts"] = true
        properties["hibernate.order_updates"] = true
        
        // Disable second-level cache for tests
        properties["hibernate.cache.use_second_level_cache"] = false
        properties["hibernate.cache.use_query_cache"] = false
        
        // Optimize schema generation for tests
        properties["hibernate.hbm2ddl.auto"] = "create-drop"
        properties["hibernate.show_sql"] = false
        properties["hibernate.format_sql"] = false
        
        // Use in-memory database for tests
        properties["hibernate.connection.provider_class"] = "org.hibernate.hikaricp.internal.HikariCPConnectionProvider"
        properties["hibernate.hikari.minimumIdle"] = 1
        properties["hibernate.hikari.maximumPoolSize"] = 5
        
        return properties
    }
} 