package io.tolgee.ee.data.glossary

import jakarta.validation.constraints.Size

class GlossaryHighlightsRequest {
  @field:Size(max = 65536)
  val text: String = ""
}
