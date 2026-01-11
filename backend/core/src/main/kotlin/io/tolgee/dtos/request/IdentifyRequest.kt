package io.tolgee.dtos.request

import jakarta.validation.constraints.NotBlank

data class IdentifyRequest(
  @NotBlank
  var anonymousUserId: String = "",
)
