package io.tolgee.component.machineTranslation

import io.tolgee.component.machineTranslation.metadata.MtMetadata
import io.tolgee.service.machineTranslation.MtServiceInfo

data class TranslationParams(
  val text: String,
  val textRaw: String,
  val keyName: String?,
  val sourceLanguageTag: String,
  val targetLanguageTag: String,
  val metadata: MtMetadata? = null,
  val serviceInfo: MtServiceInfo,
  val isBatch: Boolean,
  var pluralForms: Map<String, String>? = null,
  val pluralFormExamples: Map<String, String>? = null,
)
