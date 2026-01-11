package io.tolgee.dtos.misc

import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.model.Project
import io.tolgee.model.enums.ProjectPermissionType

data class CreateProjectInvitationParams(
  var project: Project,
  var type: ProjectPermissionType?,
  var languagePermissions: LanguagePermissions =
    LanguagePermissions(
      null,
      null,
      null,
      null,
    ),
  var scopes: Set<String>? = null,
  override val email: String? = null,
  override val name: String? = null,
  override val agencyId: Long? = null,
) : CreateInvitationParams
