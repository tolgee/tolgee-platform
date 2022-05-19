package io.tolgee.configuration

import io.tolgee.activity.ActivityHolder
import io.tolgee.configuration.TransactionScopeConfig.Companion.SCOPE_TRANSACTION
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.ScopeNotActiveException
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.annotation.RequestScope

@Configuration
class ActivityHolderConfig {
  @Bean
  @Scope(SCOPE_TRANSACTION, proxyMode = ScopedProxyMode.TARGET_CLASS)
  @ConditionalOnMissingBean
  @Qualifier("transactionActivityHolder")
  fun transactionActivityHolder(applicationContext: ApplicationContext): ActivityHolder {
    return ActivityHolder(applicationContext)
  }

  @Bean
  @RequestScope
  @Qualifier("requestActivityHolder")
  fun requestActivityHolder(applicationContext: ApplicationContext): ActivityHolder {
    return ActivityHolder(applicationContext)
  }

  @Bean
  @Primary
  @Scope(BeanDefinition.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
  fun activityHolder(applicationContext: ApplicationContext): ActivityHolder {
    return try {
      applicationContext.getBean("requestActivityHolder", ActivityHolder::class.java).also {
        it.activityRevision
      }
    } catch (e: ScopeNotActiveException) {
      return applicationContext.getBean("transactionActivityHolder", ActivityHolder::class.java)
    }
  }
}
