package io.tolgee.configuration

import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.hibernate5.LocalSessionFactoryBean
import javax.sql.DataSource
//
//@Configuration
//class HibernateSessionConfiguration(
//  private val dataSource: DataSource
//) {
//  @Bean
//  fun sessionFactory(emf: EntityManagerFactory?): LocalSessionFactoryBean {
//    val sessionFactory = LocalSessionFactoryBean()
//    sessionFactory.setDataSource(dataSource)
//    sessionFactory.setPackagesToScan("io.tolgee.model")
//    sessionFactory.hibernateProperties = hibernateProperties()
//    return sessionFactory
//  }
//}
