package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(itemRelation = "branchMerge")
open class BranchMergeRefModel(
  @Schema(description = "Branch merge ID")
  val id: Long,
  @Schema(description = "Target branch name")
  val targetBranchName: String,
  @Schema(description = "Date of merge. If null, merge was not applied yet")
  val mergedAt: Long? = null,
) : RepresentationModel<BranchMergeRefModel>()
