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
  @Schema(
    description =
      "Create the branch protected: edits, imports, merges and updates into it then require " +
        "BRANCH_PROTECTED_MODIFY",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  val protected: Boolean = false,
) : RepresentationModel<CreateBranchModel>()
