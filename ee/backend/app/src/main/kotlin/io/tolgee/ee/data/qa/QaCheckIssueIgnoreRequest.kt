package io.tolgee.ee.data.qa

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class QaCheckPreviewRequest {
  @field:Size(max = 65536)
  val text: String = ""

  @Schema(example = "cs-CZ", description = "Language tag according to BCP 47 definition")
  @field:NotBlank
  @field:Size(max = 20)
  @field:Pattern(regexp = "^[^,]*$", message = "can not contain coma")
  val languageTag: String = ""

  val keyId: Long? = null
}
