package io.tolgee.api.v2.hateoas.permission

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

interface IPermissionModel {
  @get:Schema(
    description = "Permitted scopes granted to user. When user has type permissions, this field contains permission scopes of the type.",
    example = """["KEYS_EDIT", "TRANSLATIONS_VIEW"]"""
  )
  val scopes: Array<Scope>

  @get:Schema(
    description = "The user permission type. (Null if uses granular permissions)",
  )
  val type: ProjectPermissionType?

  @get:Schema(
    description = """Deprecated (use translateLanguageIds). 

List of languages current user has TRANSLATE permission to. If null, all languages edition is permitted.""",
    deprecated = true,
    example = "[200001, 200004]"
  )
  val permittedLanguageIds: Collection<Long>?

  @get:Schema(
    description = """List of languages user can translate to. If null, all languages edition is permitted.""",
    example = "[200001, 200004]"
  )
  val translateLanguageIds: Collection<Long>?

  @get:Schema(
    description = """List of languages user can view. If null, all languages edition is permitted.""",
    example = "[200001, 200004]"
  )
  val viewLanguageIds: Collection<Long>?

  @get:Schema(
    description = """List of languages user can change state to. If null, all languages edition is permitted.""",
    example = "[200001, 200004]"
  )
  val stateChangeLanguageIds: Collection<Long>?

  @get:Schema(
    description = "Has user explicitly set granular permissions?",
  )
  val granular: Boolean
}
