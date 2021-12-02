package io.tolgee.dtos

data class TranslationMemoryItem(
  var targetText: String,
  var baseText: String,
  var keyName: String,
  var match: Float
)
