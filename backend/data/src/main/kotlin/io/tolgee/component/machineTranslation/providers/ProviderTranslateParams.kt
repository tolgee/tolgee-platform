package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.metadata.Metadata

data class ProviderTranslateParams(
  val text: String,
  val textRaw: String,
  val keyName: String?,
  var sourceLanguageTag: String,
  var targetLanguageTag: String,
  val metadata: Metadata? = null
)
