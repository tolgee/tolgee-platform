package io.tolgee.dtos.response

import io.tolgee.model.Project
import io.tolgee.model.enums.Scope

open class ProjectDTO(
  var id: Long? = null,
  var name: String? = null,
  var scopes: Array<Scope>,
) {
  companion object {
    @JvmStatic
    fun fromEntityAndPermission(
      project: Project,
      scopes: Array<Scope>,
    ): ProjectDTO {
      return ProjectDTO(project.id, project.name, scopes)
    }
  }
}
