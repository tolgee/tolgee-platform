package io.tolgee.configuration

import io.tolgee.configuration.TransactionScopeConfig.Companion.SCOPE_TRANSACTION
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.ProjectService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
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
  @ConditionalOnMissingBean
  fun transactionProjectHolder(projectService: ProjectService): ProjectHolder {
    return ProjectHolder(projectService)
  }

  @Bean
  @RequestScope
  @Primary
  fun requestProjectHolder(projectService: ProjectService): ProjectHolder {
    return ProjectHolder(projectService)
  }
}
