package io.tolgee.component.machineTranslation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.constants.MtServiceType

data class TranslationParams(
  val text: String,
  val sourceLanguageTag: String,
  val targetLanguageTag: String,
  val serviceType: MtServiceType
) {
  val cacheKey: String
    get() = jacksonObjectMapper()
      .writeValueAsString(listOf(text, sourceLanguageTag, targetLanguageTag, serviceType))
}
