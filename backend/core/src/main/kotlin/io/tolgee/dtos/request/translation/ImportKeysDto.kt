package io.tolgee.dtos.request.translation

import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated

@Validated
data class ImportKeysDto(
  @field:Valid
  val keys: List<ImportKeysItemDto> = listOf(),
)
