package io.tolgee.dtos.request.project

import io.tolgee.model.Language

class LanguagePermissions(
  var translate: Set<Language>?,
  var view: Set<Language>?,
  var stateChange: Set<Language>?,
)
