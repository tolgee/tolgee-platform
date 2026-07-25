package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ProjectPermissionType

data class ProjectInviteContributorDto(
  @Schema(description = "Id of the contributor to invite as a member")
  val userId: Long,
  val type: ProjectPermissionType? = null,
  @get:Schema(
    description = "Granted scopes for the invited user",
    example = """["translations.view", "translations.edit"]""",
  )
  val scopes: Set<String>? = null,
  override var languages: Set<Long>? = null,
  override var translateLanguages: Set<Long>? = null,
  override var viewLanguages: Set<Long>? = null,
  override var stateChangeLanguages: Set<Long>? = null,
  override var suggestLanguages: Set<Long>? = null,
  override var suggestManageLanguages: Set<Long>? = null,
) : RequestWithLanguagePermissions
