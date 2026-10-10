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
  val context: String? = null,
  val serviceInfo: MtServiceInfo,
  val isBatch: Boolean,
  var pluralForms: Map<String, String>? = null,
  val pluralFormExamples: Map<String, String>? = null,
  /**
   * True when [text] contains the `<x id="tolgee-number">` tag protecting the ICU `#` plural
   * placeholder (see [io.tolgee.service.machineTranslation.PluralTranslationUtil]). Providers use this
   * to switch on their own tag-preserving/HTML mode so the engine doesn't mangle the tag.
   */
  val containsNumberTag: Boolean = false,
)
