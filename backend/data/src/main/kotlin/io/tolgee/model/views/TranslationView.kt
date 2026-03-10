package io.tolgee.model.views

import io.tolgee.constants.MtServiceType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Label

data class TranslationView(
  val id: Long?,
  val text: String?,
  val state: TranslationState,
  val auto: Boolean,
  val mtProvider: MtServiceType?,
  val commentCount: Long,
  val unresolvedCommentCount: Long,
  val outdated: Boolean,
  var labels: List<Label> = emptyList(),
  val activeSuggestionCount: Long,
  val totalSuggestionCount: Long,
  val qaIssueCount: Long = 0,
  val qaChecksStale: Boolean = false,
) {
  var suggestions: List<TranslationSuggestionView>? = null
  var qaIssues: List<TranslationQaIssue> = emptyList()
}
