package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ProfileVisibility
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserUpdateRequestDto(
  @field:NotBlank
  var name: String = "",
  @field:NotBlank
  var email: String = "",
  @field:Size(max = 50)
  var currentPassword: String? = null,
  @Schema(
    description =
      "Callback url for link sent in e-mail." +
        " This may be omitted, when server has set frontEndUrl in properties.",
  )
  var callbackUrl: String? = null,
  @Schema(description = "Who can open the user's community profile. Omitted = unchanged.")
  var profileVisibility: ProfileVisibility? = null,
)
