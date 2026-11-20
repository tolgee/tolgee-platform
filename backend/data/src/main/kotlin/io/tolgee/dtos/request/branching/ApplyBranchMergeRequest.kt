package io.tolgee.dtos.request.branching

import io.swagger.v3.oas.annotations.media.Schema

data class ApplyBranchMergeRequest(
  @Schema(
    description =
      "Delete the source branch after the merge. When omitted, the branch is deleted unless the merge " +
        "was selective (filters or excluded changes), in which case it is kept so the held-back changes stay pending.",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  val deleteBranch: Boolean? = null,
)
