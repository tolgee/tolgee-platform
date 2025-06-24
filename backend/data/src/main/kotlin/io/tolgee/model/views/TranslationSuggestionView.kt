package io.tolgee.model.views

import io.tolgee.model.enums.TranslationSuggestionState

interface TranslationSuggestionView {
  var id: Long
  var keyId: Long
  var languageId: Long
  var languageTag: String
  var userId: Long
  var translation: String?
  var state: TranslationSuggestionState
}
