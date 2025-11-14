package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import org.springframework.hateoas.RepresentationModel

@Suppress("unused")
open class CreateBranchModel(
  @Schema(description = "Branch name, example = feature/new-feature")
  @field:Pattern(regexp = "^[a-z0-9]([a-z0-9-_/]*[a-z0-9])?$", message = "invalid_pattern")
  @field:Length(min = 2, max = 100, message = "max_length")
  val name: String,
  @Schema(description = "Origin branch id")
  val originBranchId: Long,
) : RepresentationModel<CreateBranchModel>()
