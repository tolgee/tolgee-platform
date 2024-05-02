package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

class LanguageMapping(
  @Schema(
    description =
      "The language from the imported file. This is only case of files supporting multiple languages " +
        "like XLIFF, where you specify the source and target languages. " +
        "For files containing single language values, keep this `null`.",
    example = "en-US",
  )
  val importFileLanguage: String? = null,
  @Schema(
    description =
      "The tag of language existing in the Tolgee platform to which the imported " +
        "language should be mapped.",
    example = "en-US",
  )
  val existingLanguageTag: String,
)
