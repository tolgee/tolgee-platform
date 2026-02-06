package io.tolgee.dtos

import io.tolgee.model.enums.LlmProviderType

data class BatchTranslateInfoDto(
  val available: Boolean,
  val discountPercent: Double?,
  val userChoiceAllowed: Boolean,
  val providerType: LlmProviderType?,
)
