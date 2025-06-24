package io.tolgee.dtos.request.translation.label

import jakarta.validation.constraints.NotNull

data class TranslationLabelRequest(
  @field:NotNull
  val keyId: Long,
  @field:NotNull
  val languageId: Long,
  @field:NotNull
  val labelId: Long,
)
