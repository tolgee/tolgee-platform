package io.tolgee.dtos.request.translation

import io.swagger.v3.oas.annotations.media.Schema

class KeyCodeReferenceRequest(
  @Schema(description = "Path to the file where the key is used", example = "src/components/Header.tsx")
  val path: String = "",
  @Schema(description = "Line number in the file where the key is used", example = "42")
  val line: Long? = null,
)
