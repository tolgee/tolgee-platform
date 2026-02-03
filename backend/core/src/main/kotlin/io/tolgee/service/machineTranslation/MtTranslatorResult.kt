package io.tolgee.service.machineTranslation

import io.tolgee.constants.MtServiceType

data class MtTranslatorResult(
  var translatedText: String?,
  val actualPrice: Int,
  val contextDescription: String? = null,
  val service: MtServiceType,
  val promptId: Long?,
  val targetLanguageId: Long,
  val baseBlank: Boolean,
  val exception: Exception?,
)
