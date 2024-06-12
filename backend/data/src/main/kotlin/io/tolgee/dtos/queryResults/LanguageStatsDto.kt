package io.tolgee.dtos.queryResults

import io.tolgee.api.ILanguageStats

data class LanguageStatsDto(
  override val languageId: Long,
  val projectId: Long,
  override val untranslatedWords: Long,
  override val translatedWords: Long,
  override val reviewedWords: Long,
  override val untranslatedKeys: Long,
  override val translatedKeys: Long,
  override val reviewedKeys: Long,
  override val untranslatedPercentage: Double,
  override val translatedPercentage: Double,
  override val reviewedPercentage: Double,
) : ILanguageStats
