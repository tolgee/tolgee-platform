package io.tolgee.security.project_auth

import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project
import io.tolgee.service.ProjectService

open class ProjectHolder(
  private val projectService: ProjectService
) {
  open val projectEntity: Project by lazy {
    projectService.get(project.id)
  }

  open lateinit var project: ProjectDto
}
