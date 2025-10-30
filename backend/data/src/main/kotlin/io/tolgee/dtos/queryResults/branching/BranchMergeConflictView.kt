package io.tolgee.dtos.queryResults.branching

import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.views.KeyWithTranslationsView

data class BranchMergeConflictView(
  val id: Long,
  val sourceBranchKeyId: Long,
  val targetBranchKeyId: Long,
  val resolutionType: BranchKeyMergeResolutionType?,
) {
  lateinit var sourceBranchKey: KeyWithTranslationsView
  lateinit var targetBranchKey: KeyWithTranslationsView
}
