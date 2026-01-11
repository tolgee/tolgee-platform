package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ProjectPermissionType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class ProjectInviteUserDto(
  var type: ProjectPermissionType? = null,
  @get:Schema(
    description = "Granted scopes for the invited user",
    example = """["translations.view", "translations.edit"]""",
  )
  var scopes: Set<String>? = null,
  override var languages: Set<Long>? = null,
  override var translateLanguages: Set<Long>? = null,
  override var viewLanguages: Set<Long>? = null,
  override var stateChangeLanguages: Set<Long>? = null,
  override var suggestLanguages: Set<Long>? = null,
  @Schema(
    description = """Email to send invitation to""",
  )
  @field:Size(max = 250)
  @field:Email
  val email: String? = null,
  @Schema(
    description = """Name of invited user""",
  )
  @field:Size(max = 250)
  val name: String? = null,
  @Schema(
    description = """Id of invited agency""",
  )
  val agencyId: Long? = null,
) : RequestWithLanguagePermissions
