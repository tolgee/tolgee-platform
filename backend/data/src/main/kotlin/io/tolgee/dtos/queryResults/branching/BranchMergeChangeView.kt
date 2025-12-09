package io.tolgee.dtos.queryResults.branching

import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.views.KeyWithTranslationsView

data class BranchMergeChangeView(
  val id: Long,
  val changeType: BranchKeyMergeChangeType,
  val resolutionType: BranchKeyMergeResolutionType?,
  val sourceBranchKeyId: Long?,
  val targetBranchKeyId: Long?,
) {
  var sourceBranchKey: KeyWithTranslationsView? = null
  var targetBranchKey: KeyWithTranslationsView? = null
}
