package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeChangeView
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeChangeModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchMergeChangeModelAssembler(
  private val branchMergeKeyModelAssembler: BranchMergeKeyModelAssembler,
) : RepresentationModelAssembler<BranchMergeChangeView, BranchMergeChangeModel> {
  override fun toModel(entity: BranchMergeChangeView): BranchMergeChangeModel {
    return BranchMergeChangeModel(
      id = entity.id,
      type = entity.changeType,
      sourceKey = entity.sourceBranchKey?.let { branchMergeKeyModelAssembler.toModel(it, entity.allowedLanguageTags) },
      mergedKey = entity.mergedBranchKey?.let { branchMergeKeyModelAssembler.toModel(it, entity.allowedLanguageTags) },
      targetKey = entity.targetBranchKey?.let { branchMergeKeyModelAssembler.toModel(it, entity.allowedLanguageTags) },
      changedTranslations = entity.changedTranslations,
      resolution = entity.resolutionType,
      effectiveResolution = entity.effectiveResolutionType ?: entity.resolutionType,
    )
  }
}
