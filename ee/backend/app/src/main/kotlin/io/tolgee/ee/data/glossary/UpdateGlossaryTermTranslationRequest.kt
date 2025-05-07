package io.tolgee.ee.data.glossary

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class UpdateGlossaryTermTranslationRequest {
  @Schema(example = "Translated text to language of languageTag", description = "Translation text")
  @field:Size(max = 50)
  var text: String = ""

  @Schema(example = "cs-CZ", description = "Language tag according to BCP 47 definition")
  @field:NotBlank
  @field:Size(max = 20)
  @field:Pattern(regexp = "^[^,]*$", message = "can not contain coma")
  var languageTag: String = ""
}
