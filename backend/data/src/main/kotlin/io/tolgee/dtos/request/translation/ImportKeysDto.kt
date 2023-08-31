package io.tolgee.dtos.request.translation

import org.springframework.validation.annotation.Validated
import javax.validation.Valid

@Validated
data class ImportKeysDto(
  @field:Valid
  val keys: List<ImportKeysItemDto> = listOf()
)
