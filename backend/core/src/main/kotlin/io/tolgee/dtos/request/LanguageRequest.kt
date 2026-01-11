package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LanguageRequest(
  @Schema(example = "Czech", description = "Language name in english")
  @field:NotBlank
  @field:Size(max = 100)
  var name: String = "",
  @Schema(example = "čeština", description = "Language name in this language")
  @field:NotBlank
  @field:Size(max = 100)
  var originalName: String? = null,
  @Schema(example = "cs-CZ", description = "Language tag according to BCP 47 definition")
  @field:NotBlank
  @field:Size(max = 20)
  @field:Pattern(regexp = "^[^,]*$", message = "can not contain coma")
  var tag: String = "",
  @Schema(example = "\uD83C\uDDE8\uD83C\uDDFF", description = "Language flag emoji as UTF-8 emoji")
  @field:Size(max = 20)
  var flagEmoji: String? = null,
)
