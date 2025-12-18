package io.tolgee.dtos.queryResults.branching

import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key

data class BranchMergeChangeView(
  val id: Long,
  val changeType: BranchKeyMergeChangeType,
  val resolutionType: BranchKeyMergeResolutionType?,
  val sourceBranchKeyId: Long?,
  val targetBranchKeyId: Long?,
) {
  var sourceBranchKey: Key? = null
  var targetBranchKey: Key? = null
  var mergedBranchKey: Key? = null
  var changedTranslations: List<String>? = null
  var effectiveResolutionType: BranchKeyMergeResolutionType? = null
  var allowedLanguageTags: Set<String>? = null
}
