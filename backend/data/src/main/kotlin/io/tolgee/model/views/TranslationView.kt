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
  // Comment, suggestion and QA counts are populated by batched post-loading in
  // TranslationViewDataProvider. They are `var` so the post-load step can overwrite the
  // initial 0/false values.
  var commentCount: Long,
  var unresolvedCommentCount: Long,
  val outdated: Boolean,
  var labels: List<Label> = emptyList(),
  var activeSuggestionCount: Long,
  var totalSuggestionCount: Long,
  var qaIssueCount: Long = 0,
  var qaChecksStale: Boolean = false,
) {
  var suggestions: List<TranslationSuggestionView>? = null
  var qaIssues: List<TranslationQaIssue> = emptyList()
}
