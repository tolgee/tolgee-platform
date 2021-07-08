package io.tolgee.dtos.response

import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.Project

open class ProjectDTO(
  var id: Long? = null,
  var name: String? = null,
  var permissionType: ProjectPermissionType? = null
) {

  companion object {
    @JvmStatic
    fun fromEntityAndPermission(project: Project, permissionType: ProjectPermissionType): ProjectDTO {
      return ProjectDTO(project.id, project.name, permissionType)
    }
  }
}
