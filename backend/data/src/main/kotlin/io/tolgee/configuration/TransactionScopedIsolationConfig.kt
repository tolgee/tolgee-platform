package io.tolgee.configuration

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@Configuration
class TransactionScopedIsolationConfig : BeanPostProcessor {
  override fun postProcessBeforeInitialization(
    bean: Any,
    beanName: String,
  ): Any {
    if (bean is LocalContainerEntityManagerFactoryBean) {
      bean.jpaDialect = TransactionScopedIsolationJpaDialect()
    }
    return bean
  }
}
