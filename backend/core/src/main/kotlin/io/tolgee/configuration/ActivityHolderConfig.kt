package io.tolgee.configuration

import io.tolgee.activity.ActivityHolder
import io.tolgee.component.ActivityHolderProvider
import io.tolgee.configuration.TransactionScopeConfig.Companion.SCOPE_TRANSACTION
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanDefinition
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

  /**
   * This method for getting the activity holder is slow, since it
   * needs to create new bean every time holder is requested. Which is pretty often.
   *
   * Use the activityHolderProvider when possible.
   */
  @Bean
  @Primary
  @Scope(BeanDefinition.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
  fun activityHolder(activityHolderProvider: ActivityHolderProvider): ActivityHolder {
    return activityHolderProvider.getActivityHolder()
  }
}
