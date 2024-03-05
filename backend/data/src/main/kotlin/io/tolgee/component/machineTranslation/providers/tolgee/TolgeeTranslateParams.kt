package io.tolgee.component.machineTranslation.providers.tolgee

import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.model.mtServiceConfig.Formality

class TolgeeTranslateParams(
  val text: String,
  val keyName: String?,
  val sourceTag: String,
  val targetTag: String,
  val metadata: Metadata?,
  val formality: Formality?,
  val isBatch: Boolean,
  val pluralForms: Map<String, String>? = null,
  val pluralFormExamples: Map<String, String>? = null,
)
