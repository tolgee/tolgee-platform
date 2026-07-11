package io.tolgee.ee.data.translationAgency

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateTranslationAgencyRequest(
  @field:NotBlank
  @field:Size(min = 3, max = 255)
  var name: String = "",
  @field:Size(min = 0, max = 2000)
  var description: String = "",
  var services: List<String> = emptyList(),
  @field:Size(min = 0, max = 255)
  var url: String = "",
  @field:Size(min = 3, max = 255)
  var email: String = "",
  var emailBcc: List<String> = emptyList(),
)
