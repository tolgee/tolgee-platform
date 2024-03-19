package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.model.mtServiceConfig.Formality

data class ProviderTranslateParams(
  val text: String,
  val textRaw: String,
  val keyName: String?,
  var sourceLanguageTag: String,
  var targetLanguageTag: String,
  val metadata: Metadata? = null,
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
)
