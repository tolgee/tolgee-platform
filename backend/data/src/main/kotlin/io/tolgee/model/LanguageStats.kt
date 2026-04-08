/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.model

import io.tolgee.api.ILanguageStats
import io.tolgee.dtos.queryResults.LanguageStatsDto
import io.tolgee.model.branching.Branch
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.ColumnDefault
import java.util.Date

@Entity
@Table
class LanguageStats(
  @ManyToOne(fetch = FetchType.LAZY)
  val language: Language,
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  var branch: Branch? = null,
) : StandardAuditModel(),
  ILanguageStats {
  override var untranslatedWords: Long = 0

  override var translatedWords: Long = 0

  override var reviewedWords: Long = 0

  override var untranslatedKeys: Long = 0

  override var translatedKeys: Long = 0

  override var reviewedKeys: Long = 0

  override var untranslatedPercentage: Double = 0.0

  override var translatedPercentage: Double = 0.0

  override var reviewedPercentage: Double = 0.0

  @Temporal(TemporalType.TIMESTAMP)
  override var translationsUpdatedAt: Date? = null

  @ColumnDefault("0")
  override var qaIssueCount: Long = 0

  @ColumnDefault("0")
  override var qaChecksStaleCount: Long = 0

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
      translationsUpdatedAt = translationsUpdatedAt,
      qaIssueCount = qaIssueCount,
      qaChecksStaleCount = qaChecksStaleCount,
      isDefaultBranch = branch?.isDefault,
    )
}
