package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.ee.api.v2.validation.ValidBranchName

data class RenameBranchModel(
  @Schema(description = "New branch name", example = "feature/rename-branch")
  @field:ValidBranchName
  val name: String,
)
