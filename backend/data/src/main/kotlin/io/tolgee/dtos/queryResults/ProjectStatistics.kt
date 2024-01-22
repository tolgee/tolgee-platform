package io.tolgee.dtos.queryResults

import io.tolgee.model.enums.TranslationState
import java.math.BigDecimal

data class ProjectStatistics(
  val projectId: Long,
  val keyCount: Long,
  val languageCount: Long,
  val translationStatePercentages: Map<TranslationState, BigDecimal>,
)
