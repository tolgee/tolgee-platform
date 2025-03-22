package io.tolgee.configuration

import org.hibernate.SessionFactory
import org.hibernate.stat.Statistics
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.persistence.EntityManagerFactory

@Configuration
class HibernateStatisticsConfiguration {

    @Bean
    @ConditionalOnProperty(name = ["spring.jpa.properties.hibernate.generate_statistics"], havingValue = "true")
    fun hibernateStatistics(entityManagerFactory: EntityManagerFactory): Statistics {
        val sessionFactory = entityManagerFactory.unwrap(SessionFactory::class.java)
        return sessionFactory.statistics.apply { isStatisticsEnabled = true }
    }
} 