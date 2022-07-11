package io.tolgee.dtos.query_results

import io.tolgee.model.enums.TranslationState
import java.math.BigDecimal

data class ProjectStatistics(
  val projectId: Long,
  val keyCount: Long,
  val languageCount: Long,
  val translationStatePercentages: Map<TranslationState, BigDecimal>
)
