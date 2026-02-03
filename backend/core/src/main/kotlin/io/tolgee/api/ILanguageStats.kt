package io.tolgee.api

import java.util.Date

interface ILanguageStats {
  val languageId: Long
  val untranslatedWords: Long
  val translatedWords: Long
  val reviewedWords: Long
  val untranslatedKeys: Long
  val translatedKeys: Long
  val reviewedKeys: Long
  val untranslatedPercentage: Double
  val translatedPercentage: Double
  val reviewedPercentage: Double
  val translationsUpdatedAt: Date?
}
