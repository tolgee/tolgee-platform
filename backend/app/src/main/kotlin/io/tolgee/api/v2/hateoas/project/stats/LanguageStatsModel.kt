package io.tolgee.api.v2.hateoas.project.stats

open class LanguageStatsModel(
  val languageId: Long?,
  val languageTag: String?,
  val languageName: String?,
  val languageOriginalName: String?,
  val languageFlagEmoji: String?,
  val translatedKeyCount: Long,
  val translatedWordCount: Long,
  val translatedPercentage: Double,
  val reviewedKeyCount: Long,
  val reviewedWordCount: Long,
  val reviewedPercentage: Double,
)
