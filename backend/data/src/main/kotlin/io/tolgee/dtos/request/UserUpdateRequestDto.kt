package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserUpdateRequestDto(
  @field:NotBlank
  var name: String = "",
  @field:NotBlank
  var email: String = "",
  var currentPassword: String? = null,
  @Schema(
    description =
      "Callback url for link sent in e-mail." +
        " This may be omitted, when server has set frontEndUrl in properties.",
  )
  var callbackUrl: String? = null,
)
