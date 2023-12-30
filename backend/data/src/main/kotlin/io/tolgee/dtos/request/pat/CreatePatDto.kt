package io.tolgee.dtos.request.pat

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated

@Validated
data class CreatePatDto(
  @Schema(description = "Description of the PAT")
  @field:NotBlank
  @field:Length(max = 250, min = 1)
  val description: String = "",
  @Schema(
    description =
      "Expiration date in epoch format (milliseconds)." +
        " When null, token never expires.",
    example = "1661172869000",
  )
  val expiresAt: Long? = null,
)
