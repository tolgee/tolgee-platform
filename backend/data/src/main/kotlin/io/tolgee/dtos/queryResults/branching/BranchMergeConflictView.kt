package io.tolgee.dtos.queryResults.branching

import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key

data class BranchMergeConflictView(
  val id: Long,
  val sourceBranchKeyId: Long,
  val targetBranchKeyId: Long,
  val resolutionType: BranchKeyMergeResolutionType?,
) {
  lateinit var sourceBranchKey: Key
  lateinit var targetBranchKey: Key
  var mergedBranchKey: Key? = null
  var changedTranslations: List<String>? = null
  var effectiveResolutionType: BranchKeyMergeResolutionType? = null
  var allowedLanguageTags: Set<String>? = null
}
