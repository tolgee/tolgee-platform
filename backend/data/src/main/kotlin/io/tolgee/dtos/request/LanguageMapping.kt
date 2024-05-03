package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

class LanguageMapping(
  @Schema(
    description =
      "The language from the imported file.\n\n" +
        "For xliff files, this is the `source-language` or the `target-language` " +
        "attribute value of `file` element.",
    example = "en-US",
  )
  val importLanguage: String,
  @Schema(
    description =
      "The tag of language existing in the Tolgee platform to which the imported " +
        "language should be mapped.",
    example = "en-US",
  )
  val platformLanguageTag: String,
)
