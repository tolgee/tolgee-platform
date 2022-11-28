package io.tolgee.api.v2.hateoas.permission

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.Scope
import org.springframework.hateoas.RepresentationModel

open class PermissionModel(
  @Schema(
    description = "The permitted scopes",
    example = """["KEYS_EDIT", "TRANSLATIONS_VIEW"]"""
  )
  val scopes: Array<Scope>,

  @Schema(
    description = "List of languages current user has TRANSLATE permission to. " +
      "If null, all languages edition is permitted.",
    example = "[200001, 200004]"
  )
  val permittedLanguageIds: List<Long>?
) : RepresentationModel<PermissionModel>()
