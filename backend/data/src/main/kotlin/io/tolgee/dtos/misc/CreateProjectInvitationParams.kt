package io.tolgee.dtos.misc

import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project

data class CreateProjectInvitationParams(
  var project: Project,
  var type: Permission.ProjectPermissionType,
  var languages: List<Language>? = null,
  override val email: String? = null,
  override val name: String? = null
) : CreateInvitationParams
