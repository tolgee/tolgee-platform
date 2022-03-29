package io.tolgee.configuration.tolgee

import io.tolgee.activity.ActivityInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Configuration

@Configuration
class HibernateConfiguration : HibernatePropertiesCustomizer {

  @Autowired
  lateinit var activityInterceptor: ActivityInterceptor

  override fun customize(vendorProperties: MutableMap<String, Any?>) {
    vendorProperties["hibernate.ejb.interceptor"] = activityInterceptor
  }
}
