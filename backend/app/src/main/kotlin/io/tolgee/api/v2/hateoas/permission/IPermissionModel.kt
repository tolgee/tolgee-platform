package io.tolgee.api.v2.hateoas.permission

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

interface IPermissionModel {
  @get:Schema(
    description = "The permitted scopes",
    example = """["KEYS_EDIT", "TRANSLATIONS_VIEW"]"""
  )
  val scopes: Array<Scope>

  @get:Schema(
    description = "The user permission type. (Null if uses granular permissions)",
  )
  val type: ProjectPermissionType?

  @get:Schema(
    description = "List of languages current user has TRANSLATE permission to. " +
      "If null, all languages edition is permitted.",
    example = "[200001, 200004]"
  )
  val permittedLanguageIds: Collection<Long>?

  @get:Schema(
    description = "Has user explicitly set granular permissions",
  )
  val granular: Boolean
}
