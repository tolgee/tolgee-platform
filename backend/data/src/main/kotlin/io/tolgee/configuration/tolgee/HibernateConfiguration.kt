package io.tolgee.configuration.tolgee

import io.tolgee.activity.iterceptor.ActivityDatabaseInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.hibernate5.LocalSessionFactoryBean
import javax.sql.DataSource


@Configuration
class HibernateConfiguration : HibernatePropertiesCustomizer {

  @Autowired
  lateinit var activityInterceptor: ActivityDatabaseInterceptor

  override fun customize(vendorProperties: MutableMap<String, Any?>) {
    vendorProperties["hibernate.session_factory.interceptor"] = activityInterceptor
  }

}
