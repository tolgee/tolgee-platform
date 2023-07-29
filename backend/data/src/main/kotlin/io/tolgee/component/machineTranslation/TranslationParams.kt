package io.tolgee.component.machineTranslation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.constants.MtServiceType

data class TranslationParams(
  val text: String,
  val textRaw: String,
  val keyName: String?,
  val sourceLanguageTag: String,
  val targetLanguageTag: String,
  val serviceType: MtServiceType,
  val metadata: Metadata?,
  val isBatch: Boolean
) {

  val cacheKey: String
    get() = jacksonObjectMapper()
      .writeValueAsString(listOf(text, sourceLanguageTag, targetLanguageTag, serviceType, metadata))
}
