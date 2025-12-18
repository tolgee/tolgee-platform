package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeConflictModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchMergeConflictModelAssembler(
  private val branchMergeKeyModelAssembler: BranchMergeKeyModelAssembler,
) : RepresentationModelAssembler<BranchMergeConflictView, BranchMergeConflictModel> {
  override fun toModel(entity: BranchMergeConflictView): BranchMergeConflictModel {
    return BranchMergeConflictModel(
      id = entity.id,
      sourceKey = branchMergeKeyModelAssembler.toModel(entity.sourceBranchKey, entity.allowedLanguageTags),
      mergedKey = entity.mergedBranchKey?.let { branchMergeKeyModelAssembler.toModel(it, entity.allowedLanguageTags) },
      targetKey = branchMergeKeyModelAssembler.toModel(entity.targetBranchKey, entity.allowedLanguageTags),
      changedTranslations = entity.changedTranslations,
      resolution = entity.resolutionType,
      effectiveResolution = entity.effectiveResolutionType ?: entity.resolutionType,
    )
  }
}
