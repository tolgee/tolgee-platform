package io.tolgee.config

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import java.util.HashMap

class FastTestContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val properties = HashMap<String, Any>()
        
        // Disable time-consuming auto-configurations
        properties["spring.jmx.enabled"] = "false"
        properties["spring.main.banner-mode"] = "off"
        properties["spring.jackson.serialization.write-dates-as-timestamps"] = "false"
        
        // Optimize JPA
        properties["spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults"] = "false"
        properties["spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation"] = "true"
        properties["spring.jpa.properties.hibernate.dialect"] = "org.hibernate.dialect.H2Dialect"
        properties["spring.jpa.properties.hibernate.connection.provider_disables_autocommit"] = "true"
        
        // Optimize connection pool
        properties["spring.datasource.hikari.auto-commit"] = "false"
        properties["spring.datasource.hikari.connection-timeout"] = "5000"
        properties["spring.datasource.hikari.maximum-pool-size"] = "10"
        
        // Optimize logging
        properties["logging.level.root"] = "ERROR"
        properties["logging.level.org.springframework"] = "ERROR"
        properties["logging.level.org.hibernate"] = "ERROR"
        
        val propertySource = MapPropertySource("fast-test-properties", properties)
        applicationContext.environment.propertySources.addFirst(propertySource)
    }
} 