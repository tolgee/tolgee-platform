package io.tolgee.model.views

import io.tolgee.model.enums.TranslationSuggestionState
import java.util.Date

interface TranslationSuggestionView {
  var id: Long
  var keyId: Long
  var languageId: Long
  var languageTag: String
  var translation: String?
  var state: TranslationSuggestionState
  var plural: Boolean

  var authorId: Long
  var authorName: String
  var authorUsername: String
  var authorAvatarHash: String?
  var authorDeletedAt: Date?
}
