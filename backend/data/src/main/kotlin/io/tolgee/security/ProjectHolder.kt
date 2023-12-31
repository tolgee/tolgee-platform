package io.tolgee.security

import io.sentry.Sentry
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project
import io.tolgee.service.project.ProjectService

open class ProjectHolder(
  private val projectService: ProjectService,
) {
  open val projectEntity: Project by lazy {
    projectService.get(project.id)
  }

  private var _project: ProjectDto? = null
  open var project: ProjectDto
    set(value) {
      Sentry.addBreadcrumb("Project Id: ${value.id}")
      _project = value
    }
    get() {
      return _project ?: throw ProjectNotSelectedException()
    }

  val projectOrNull: ProjectDto?
    get() = _project
}
