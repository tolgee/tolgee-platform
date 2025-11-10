package io.tolgee.ee.data.glossary

import io.swagger.v3.oas.annotations.media.Schema

data class GlossaryLanguageDto(
  @Schema(
    description = "The language code (e.g., 'en' for English)",
    example = "en",
  )
  val tag: String,
  @Schema(
    description = "Indicates if this is the base (main) language of the glossary",
    example = "true",
  )
  val base: Boolean,
)
