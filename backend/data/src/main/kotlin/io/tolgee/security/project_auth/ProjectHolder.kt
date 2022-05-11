package io.tolgee.security.project_auth

import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project
import io.tolgee.service.project.ProjectService

open class ProjectHolder(
  private val projectService: ProjectService
) {
  open val projectEntity: Project by lazy {
    projectService.get(project.id)
  }

  openlateinit var project: ProjectDto
}
