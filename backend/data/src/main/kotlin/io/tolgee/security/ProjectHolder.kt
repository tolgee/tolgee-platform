package io.tolgee.security

import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project
import io.tolgee.service.project.ProjectService

@Deprecated(
  "This should be replaced with the AuthenticationFacade's provided context instead. " +
    "Services should always prefer explicit arguments rather than automatically pulling values."
)
open class ProjectHolder(
  private val projectService: ProjectService
) {
  open val projectEntity: Project by lazy {
    projectService.get(project.id)
  }

  private var _project: ProjectDto? = null
  open var project: ProjectDto
    set(value) {
      _project = value
    }
    get() {
      return _project ?: throw ProjectNotSelectedException()
    }
}
