package io.tolgee.model.views.projectStats

data class ProjectLanguageStatsResultView(
  val projectId: Long,
  val languageId: Long?,
  val languageTag: String?,
  val languageName: String?,
  val languageOriginalName: String?,
  val languageFlagEmoji: String?,
  val translatedKeys: Long,
  val translatedWords: Long,
  val reviewedKeys: Long,
  val reviewedWords: Long,
)
