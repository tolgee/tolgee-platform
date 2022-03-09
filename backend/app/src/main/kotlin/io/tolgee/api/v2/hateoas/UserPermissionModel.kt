package io.tolgee.api.v2.hateoas

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.Permission

class UserPermissionModel(
  @Schema(
    description = "List of languages current user has TRANSLATE permission to. " +
      "If null, all languages edition is permitted.",
    example = "[200001, 200004]"
  )
  val permittedLanguageIds: List<Long>?,

  @Schema(
    description = "The type of permission.",
    example = "EDIT"
  )
  val type: Permission.ProjectPermissionType
)
