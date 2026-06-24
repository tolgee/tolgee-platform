package io.tolgee.configuration

import io.tolgee.configuration.TransactionScopeConfig.Companion.SCOPE_TRANSACTION
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project
import io.tolgee.security.ProjectHolder
import io.tolgee.service.project.ProjectService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.context.request.RequestContextHolder

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

  /**
   * Dispatches to either [requestProjectHolder] or [transactionProjectHolder]
   * depending on whether a request scope is currently active.
   */
  @Bean
  @Primary
  fun projectHolder(
    applicationContext: ApplicationContext,
    // @Lazy breaks the ProjectService ↔ ProjectHolder constructor cycle; the
    // dispatcher overrides every accessor, so the field is never read.
    @Lazy projectService: ProjectService,
  ): ProjectHolder = DispatchingProjectHolder(applicationContext, projectService)

  private class DispatchingProjectHolder(
    private val applicationContext: ApplicationContext,
    projectService: ProjectService,
  ) : ProjectHolder(projectService) {
    override var project: ProjectDto
      get() = current().project
      set(value) {
        current().project = value
      }

    override val projectOrNull: ProjectDto?
      get() = current().projectOrNull

    override val projectEntity: Project
      get() = current().projectEntity

    private fun current(): ProjectHolder {
      val name =
        if (RequestContextHolder.getRequestAttributes() != null) {
          "requestProjectHolder"
        } else {
          "transactionProjectHolder"
        }
      return applicationContext.getBean(name, ProjectHolder::class.java)
    }
  }
}
