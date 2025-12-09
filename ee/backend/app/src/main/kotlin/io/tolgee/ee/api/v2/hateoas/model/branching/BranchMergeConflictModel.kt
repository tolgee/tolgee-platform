package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.translations.KeyWithTranslationsModel
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(itemRelation = "branchMergeConflict", collectionRelation = "branchMergeConflicts")
open class BranchMergeConflictModel(
  @Schema(description = "Branch merge session id")
  val id: Long,
  @Schema(description = "Source branch key")
  val sourceKey: KeyWithTranslationsModel,
  @Schema(description = "Target branch key")
  val targetKey: KeyWithTranslationsModel,
  @Schema(description = "Type of key conflict resolution")
  val resolution: BranchKeyMergeResolutionType?,
) : RepresentationModel<BranchMergeConflictModel>()
