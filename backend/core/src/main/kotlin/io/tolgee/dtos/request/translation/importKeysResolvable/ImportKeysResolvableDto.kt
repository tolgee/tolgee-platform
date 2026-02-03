package io.tolgee.dtos.request.translation.importKeysResolvable

import org.springframework.validation.annotation.Validated

@Validated
data class ImportKeysResolvableDto(
  val keys: List<ImportKeysResolvableItemDto> = listOf(),
)
