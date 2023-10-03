package io.tolgee.configuration

import io.tolgee.configuration.TransactionScopeConfig.Companion.SCOPE_TRANSACTION
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.OrganizationNotSelectedException
import io.tolgee.service.organization.OrganizationService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
import org.springframework.beans.factory.support.ScopeNotActiveException
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.annotation.RequestScope

@Configuration
class OrganizationHolderConfig {
  @Bean
  @Scope(SCOPE_TRANSACTION, proxyMode = ScopedProxyMode.TARGET_CLASS)
  @Qualifier("transactionOrganizationHolder")
  fun transactionOrganizationHolder(organizationService: OrganizationService): OrganizationHolder {
    return OrganizationHolder(organizationService)
  }

  @Bean
  @RequestScope
  @Qualifier("requestOrganizationHolder")
  fun requestOrganizationHolder(organizationService: OrganizationService): OrganizationHolder {
    return OrganizationHolder(organizationService)
  }

  @Bean
  @Primary
  @Scope(SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
  fun organizationHolder(applicationContext: ApplicationContext): OrganizationHolder {
    return try {
      applicationContext.getBean("requestOrganizationHolder", OrganizationHolder::class.java).also {
        try {
          // we must try to access something to get the exception thrown
          it.organization
        } catch (e: OrganizationNotSelectedException) {
          // ProjectNotSelectedException is normal, since org is not set initially
          return it
        }
      }
    } catch (e: ScopeNotActiveException) {
      return applicationContext.getBean("transactionOrganizationHolder", OrganizationHolder::class.java)
    }
  }
}
