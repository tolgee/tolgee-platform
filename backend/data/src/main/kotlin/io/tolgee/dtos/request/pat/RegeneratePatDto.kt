package io.tolgee.dtos.request.pat

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.validation.annotation.Validated

@Validated
data class RegeneratePatDto(
  @Schema(
    description = "Expiration date in epoch format (milliseconds). When null key never expires.",
    example = "1661172869000",
  )
  val expiresAt: Long? = null,
)
