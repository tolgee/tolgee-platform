package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.media.Schema

data class SetProjectPublicRequest(
  @Schema(description = "Whether the project should be public (discoverable and open to community suggestions)")
  var public: Boolean = false,
)
