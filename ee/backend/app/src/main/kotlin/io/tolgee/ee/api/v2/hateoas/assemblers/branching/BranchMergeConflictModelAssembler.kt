package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeConflictModel
import io.tolgee.hateoas.translations.KeyWithTranslationsModelAssembler
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchMergeConflictModelAssembler(
  private val keyWithTranslationsModelAssembler: KeyWithTranslationsModelAssembler,
) : RepresentationModelAssembler<BranchMergeConflictView, BranchMergeConflictModel> {
  override fun toModel(entity: BranchMergeConflictView): BranchMergeConflictModel {
    return BranchMergeConflictModel(
      id = entity.id,
      sourceKey = keyWithTranslationsModelAssembler.toModel(entity.sourceBranchKey),
      targetKey = keyWithTranslationsModelAssembler.toModel(entity.targetBranchKey),
      resolution = entity.resolutionType,
    )
  }
}
