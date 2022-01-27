package io.tolgee.security.project_auth

import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project
import io.tolgee.service.ProjectService
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class ProjectHolder(
  private val projectService: ProjectService
) {
  val projectEntity: Project by lazy {
    projectService.get(project.id)
  }

  lateinit var project: ProjectDto
  val isProjectInitialized
    get() = this::project.isInitialized
}
