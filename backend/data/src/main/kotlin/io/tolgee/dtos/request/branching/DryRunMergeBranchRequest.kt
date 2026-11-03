package io.tolgee.dtos.request.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.branching.BranchMergeFilters

@Suppress("unused")
open class DryRunMergeBranchRequest(
  @Schema(description = "Source branch id")
  val sourceBranchId: Long,
  @Schema(
    description =
      "Target branch id. Defaults to the origin of the source branch. " +
        "Pass a direct child of the source branch to update that child from its origin.",
  )
  val targetBranchId: Long? = null,
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val filters: BranchMergeFilters = BranchMergeFilters(),
)
