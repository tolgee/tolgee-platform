package io.tolgee.ee.service.translationMemory.tmx

data class TmxParsedEntry(
  val sourceText: String,
  val targetText: String,
  val targetLanguageTag: String,
  val tuid: String?,
)
