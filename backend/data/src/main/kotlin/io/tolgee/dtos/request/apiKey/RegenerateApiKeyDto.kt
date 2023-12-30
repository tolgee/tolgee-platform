package io.tolgee.dtos.request.apiKey

import io.swagger.v3.oas.annotations.media.Schema

data class RegenerateApiKeyDto(
  @Schema(
    description =
      "Expiration date in epoch format (milliseconds)." +
        " When null key never expires.",
    example = "1661172869000",
  )
  val expiresAt: Long? = null,
)
