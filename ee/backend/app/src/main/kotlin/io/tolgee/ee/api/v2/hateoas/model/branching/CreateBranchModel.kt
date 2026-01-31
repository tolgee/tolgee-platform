package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.ee.api.v2.validation.ValidBranchName
import org.springframework.hateoas.RepresentationModel

@Suppress("unused")
open class CreateBranchModel(
  @Schema(description = "Branch name", example = "feature/new-branch")
  @field:ValidBranchName
  val name: String,
  @Schema(description = "Origin branch id")
  val originBranchId: Long,
) : RepresentationModel<CreateBranchModel>()
