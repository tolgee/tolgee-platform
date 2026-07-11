package io.tolgee.hateoas.permission

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

interface IPermissionModel {
  @get:Schema(
    description =
      "Granted scopes to the user. When user has type permissions, " +
        "this field contains permission scopes of the type.",
    example = """["KEYS_EDIT", "TRANSLATIONS_VIEW"]""",
  )
  val scopes: Array<Scope>

  @get:Schema(
    description = "The user's permission type. This field is null if uses granular permissions",
  )
  val type: ProjectPermissionType?

  @get:Schema(
    description = """List of languages user can translate to. If null, all languages editing is permitted.""",
    example = "[200001, 200004]",
  )
  val translateLanguageIds: Collection<Long>?

  @get:Schema(
    description = """List of languages user can view. If null, all languages view is permitted.""",
    example = "[200001, 200004]",
  )
  val viewLanguageIds: Collection<Long>?

  @get:Schema(
    description =
      "List of languages user can change state to. If null, changing state " +
        "of all language values is permitted.",
    example = "[200001, 200004]",
  )
  val stateChangeLanguageIds: Collection<Long>?

  @get:Schema(
    description =
      "List of languages user can suggest to. If null, suggesting " +
        "to all languages is permitted.",
    example = "[200001, 200004]",
  )
  val suggestLanguageIds: Collection<Long>?
}
