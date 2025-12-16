package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeChangeView
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeChangeModel
import io.tolgee.hateoas.translations.KeyWithTranslationsModelAssembler
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchMergeChangeModelAssembler(
  private val keyWithTranslationsModelAssembler: KeyWithTranslationsModelAssembler,
) : RepresentationModelAssembler<BranchMergeChangeView, BranchMergeChangeModel> {
  override fun toModel(entity: BranchMergeChangeView): BranchMergeChangeModel {
    return BranchMergeChangeModel(
      id = entity.id,
      type = entity.changeType,
      sourceKey = entity.sourceBranchKey?.let { keyWithTranslationsModelAssembler.toModel(it) },
      targetKey = entity.targetBranchKey?.let { keyWithTranslationsModelAssembler.toModel(it) },
      resolution = entity.resolutionType,
    )
  }
}
