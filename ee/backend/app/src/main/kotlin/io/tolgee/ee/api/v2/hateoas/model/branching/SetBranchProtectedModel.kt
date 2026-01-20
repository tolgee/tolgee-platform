package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema

data class SetBranchProtectedModel(
  @Schema(description = "Whether the branch is protected", example = "true")
  val isProtected: Boolean,
)
