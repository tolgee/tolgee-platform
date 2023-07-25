package io.tolgee.dtos.request

import javax.validation.constraints.NotBlank

data class IdentifyRequest(
  @NotBlank
  var anonymousUserId: String = ""
)
