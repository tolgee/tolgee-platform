package io.tolgee.dtos.request.branching

import io.swagger.v3.oas.annotations.media.Schema

@Suppress("unused")
open class DryRunMergeBranchRequest(
  @Schema(description = "Name of branch merge")
  val name: String,
  @Schema(description = "Source branch id")
  val sourceBranchId: Long,
  @Schema(description = "Target branch id")
  val targetBranchId: Long,
)
