package io.tolgee.service.machineTranslation

import io.tolgee.constants.MtServiceType

data class MtBatchItemParams(
  val keyId: Long?,
  val baseTranslationText: String?,
  val targetLanguageId: Long,
  val service: MtServiceType,
  val promptId: Long? = null,
)
