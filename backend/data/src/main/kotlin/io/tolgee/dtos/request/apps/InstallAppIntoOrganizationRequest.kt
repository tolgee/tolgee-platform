package io.tolgee.dtos.request.apps

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class InstallAppIntoOrganizationRequest(
  @field:NotNull
  @Schema(description = "Id of the organization to install the app into.")
  val organizationId: Long = 0,
)
