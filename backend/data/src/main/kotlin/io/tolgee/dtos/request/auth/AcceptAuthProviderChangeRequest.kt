package io.tolgee.dtos.request.auth

import jakarta.validation.constraints.NotBlank

data class AcceptAuthProviderChangeRequest(
  @field:NotBlank
  var id: String,
)
