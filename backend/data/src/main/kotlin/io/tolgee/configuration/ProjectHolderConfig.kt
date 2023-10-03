package io.tolgee.configuration

import io.tolgee.configuration.TransactionScopeConfig.Companion.SCOPE_TRANSACTION
import io.tolgee.security.ProjectHolder
import io.tolgee.security.ProjectNotSelectedException
import io.tolgee.service.project.ProjectService
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
class ProjectHolderConfig {
  @Bean
  @Scope(SCOPE_TRANSACTION, proxyMode = ScopedProxyMode.TARGET_CLASS)
  @Qualifier("transactionProjectHolder")
  fun transactionProjectHolder(projectService: ProjectService): ProjectHolder {
    return ProjectHolder(projectService)
  }

  @Bean
  @RequestScope
  @Qualifier("requestProjectHolder")
  fun requestProjectHolder(projectService: ProjectService): ProjectHolder {
    return ProjectHolder(projectService)
  }

  @Bean
  @Primary
  @Scope(SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
  fun projectHolder(applicationContext: ApplicationContext): ProjectHolder {
    return try {
      applicationContext.getBean("requestProjectHolder", ProjectHolder::class.java).also {
        try {
          // we must try to access something to get the exception thrown
          it.project
        } catch (e: ProjectNotSelectedException) {
          // ProjectNotSelectedException is normal, since project is not set initially
          return it
        }
      }
    } catch (e: ScopeNotActiveException) {
      return applicationContext.getBean("transactionProjectHolder", ProjectHolder::class.java)
    }
  }
}
