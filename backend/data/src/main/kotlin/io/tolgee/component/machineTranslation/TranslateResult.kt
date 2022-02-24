package io.tolgee.component.machineTranslation

import io.tolgee.constants.MtServiceType

data class TranslateResult(
  var translatedText: String?,
  val actualPrice: Int = 0,
  val usedService: MtServiceType? = null
)
