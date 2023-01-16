package io.tolgee.dtos.request.translation

import org.springframework.validation.annotation.Validated

@Validated
data class ImportKeysDto(
  val keys: List<ImportKeysItemDto> = listOf()
)
