package io.tolgee.ee.data.glossary

import jakarta.validation.constraints.Size

class CreateGlossaryTermWithTranslationRequest : CreateGlossaryTermRequest() {
  @field:Size(min = 0, max = 50)
  var text: String = ""
}
