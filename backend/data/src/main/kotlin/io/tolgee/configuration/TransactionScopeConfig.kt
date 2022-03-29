package io.tolgee.configuration

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.SimpleTransactionScope

@Configuration
class TransactionScopeConfig : BeanFactoryPostProcessor {
  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    beanFactory.registerScope(SCOPE_TRANSACTION, SimpleTransactionScope())
  }

  companion object {
    const val SCOPE_TRANSACTION = "txScope"
  }
}
