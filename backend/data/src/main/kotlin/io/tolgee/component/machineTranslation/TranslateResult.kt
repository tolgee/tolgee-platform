package io.tolgee.component.machineTranslation

import io.tolgee.constants.MtServiceType
import java.io.Serializable

data class TranslateResult(
  var translatedText: String?,
  var contextDescription: String?,
  var actualPrice: Int = 0,
  val usedService: MtServiceType? = null,
  val baseBlank: Boolean,
  val exception: Exception? = null,
) : Serializable
