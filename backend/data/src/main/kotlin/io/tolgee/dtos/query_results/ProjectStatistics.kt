package io.tolgee.dtos.query_results

import io.tolgee.model.enums.TranslationState

data class ProjectStatistics(
  val projectId: Long,
  val keyCount: Long,
  val languageCount: Long,
  val translationStateCounts: Map<TranslationState, Long>
)
