/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import io.tolgee.api.ILanguageStats
import io.tolgee.dtos.queryResults.LanguageStatsDto
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["language_id"], name = "language_stats_language_id_key")])
class LanguageStats(
  @OneToOne(fetch = FetchType.LAZY)
  val language: Language,
) : StandardAuditModel(), ILanguageStats {
  override var untranslatedWords: Long = 0

  override var translatedWords: Long = 0

  override var reviewedWords: Long = 0

  override var untranslatedKeys: Long = 0

  override var translatedKeys: Long = 0

  override var reviewedKeys: Long = 0

  override var untranslatedPercentage: Double = 0.0

  override var translatedPercentage: Double = 0.0

  override var reviewedPercentage: Double = 0.0
  override val languageId: Long
    get() = language.id

  fun toDto() =
    LanguageStatsDto(
      languageId = language.id,
      projectId = language.project.id,
      untranslatedWords = untranslatedWords,
      translatedWords = translatedWords,
      reviewedWords = reviewedWords,
      untranslatedKeys = untranslatedKeys,
      translatedKeys = translatedKeys,
      reviewedKeys = reviewedKeys,
      untranslatedPercentage = untranslatedPercentage,
      translatedPercentage = translatedPercentage,
      reviewedPercentage = reviewedPercentage,
    )
}
