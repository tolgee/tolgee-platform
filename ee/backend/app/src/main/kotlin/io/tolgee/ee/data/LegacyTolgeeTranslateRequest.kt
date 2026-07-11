package io.tolgee.ee.data

import io.tolgee.model.mtServiceConfig.Formality

data class LegacyTolgeeTranslateRequest(
  val text: String,
  val keyName: String?,
  val sourceTag: String,
  val targetTag: String,
  val metadata: Metadata?,
  val formality: Formality?,
  val isBatch: Boolean,
  val pluralForms: Map<String, String>? = null,
  val pluralFormExamples: Map<String, String>? = null,
) {
  data class Metadata(
    val examples: List<ExampleItem> = emptyList(),
    val closeItems: List<ExampleItem> = emptyList(),
    val keyDescription: String?,
    val projectDescription: String?,
    val languageDescription: String?,
  )

  data class ExampleItem(
    val source: String,
    val target: String,
    val key: String,
    val keyNamespace: String?,
  )
}
