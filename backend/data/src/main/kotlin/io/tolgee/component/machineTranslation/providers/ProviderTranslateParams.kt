package io.tolgee.component.machineTranslation.providers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.machineTranslation.metadata.MtMetadata
import io.tolgee.model.mtServiceConfig.Formality

data class ProviderTranslateParams(
  val text: String,
  val textRaw: String,
  val keyName: String?,
  var sourceLanguageTag: String,
  var targetLanguageTag: String,
  var metadata: MtMetadata? = null,
  val formality: Formality? = null,
  /**
   * Whether translation is executed as a part of batch translation task
   */
  val isBatch: Boolean,
  /**
   * Only for translators supporting plurals
   */
  val pluralForms: Map<String, String>? = null,
  /**
   * Only for translators supporting plurals
   */
  val pluralFormExamples: Map<String, String>? = null,
) {
  fun cacheKey(provider: String): String {
    return jacksonObjectMapper()
      .writeValueAsString(
        listOf(
          text,
          textRaw,
          keyName,
          sourceLanguageTag,
          targetLanguageTag,
          metadata,
          formality,
          pluralForms,
          pluralFormExamples,
          provider,
        ),
      )
  }
}
