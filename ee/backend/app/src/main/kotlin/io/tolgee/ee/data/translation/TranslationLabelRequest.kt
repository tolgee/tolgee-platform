package io.tolgee.ee.data.translation

import jakarta.validation.constraints.NotNull

data class TranslationLabelRequest(
  @field:NotNull
  val keyId: Long,
  @field:NotNull
  val languageId: Long,
  @field:NotNull
  val labelId: Long,
)
