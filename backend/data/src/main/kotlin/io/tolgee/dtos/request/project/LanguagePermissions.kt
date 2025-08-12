package io.tolgee.dtos.request.project

import io.tolgee.model.Language

class LanguagePermissions(
  var translate: Set<Language>? = null,
  var view: Set<Language>? = null,
  var stateChange: Set<Language>? = null,
  var suggest: Set<Language>? = null,
)
