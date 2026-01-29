package io.tolgee.dtos.request.branching

import io.swagger.v3.oas.annotations.media.Schema

@Suppress("unused")
open class DryRunMergeBranchRequest(
  @Schema(description = "Source branch id")
  val sourceBranchId: Long,
)
