package io.tolgee.hateoas.permission

import io.swagger.v3.oas.annotations.media.Schema

interface IDeprecatedPermissionModel : IPermissionModel {
  @get:Schema(
    description = """Deprecated (use translateLanguageIds). 

List of languages current user has TRANSLATE permission to. If null, all languages edition is permitted.""",
    deprecated = true,
    example = "[200001, 200004]",
  )
  val permittedLanguageIds: Collection<Long>?
}
