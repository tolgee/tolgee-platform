package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.media.Schema

interface RequestWithLanguagePermissions {
  @get:Schema(deprecated = true, description = "Deprecated -> use translate languages")
  var languages: Set<Long>?

  @get:Schema(description = "Languages user can translate to")
  var translateLanguages: Set<Long>?

  @get:Schema(description = "Languages user can view")
  var viewLanguages: Set<Long>?

  @get:Schema(description = "Languages user can change translation state (review)")
  var stateChangeLanguages: Set<Long>?

  @get:Schema(description = "Languages user can suggest translation")
  var suggestLanguages: Set<Long>?
}
