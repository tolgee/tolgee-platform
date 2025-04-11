package io.tolgee.ee.data.glossary

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class UpdateGlossaryTermTranslationRequest {
  @field:Size(min = 0, max = 50)
  var text: String = ""

  @field:NotBlank // TODO: if it stays as code we need stricter validation here
  var languageCode: String = ""
}
