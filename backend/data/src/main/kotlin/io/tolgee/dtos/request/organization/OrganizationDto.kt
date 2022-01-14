package io.tolgee.dtos.request.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.Permission
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class OrganizationDto(
  @field:NotBlank @field:Size(min = 3, max = 50)
  @Schema(example = "Beautiful organization")
  var name: String? = null,

  @Schema(example = "This is a beautiful organization full of beautiful and clever people")
  var description: String? = null,

  @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
  @Schema(example = "btforg")
  var slug: String? = null,

  @Enumerated(EnumType.STRING)
  var basePermissions: Permission.ProjectPermissionType = Permission.ProjectPermissionType.VIEW,
)
