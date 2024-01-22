package io.tolgee.service.export.dataProvider

import io.tolgee.model.enums.TranslationState

class ExportTranslationView(
  val id: Long?,
  val text: String?,
  val state: TranslationState,
  val key: ExportKeyView,
  val languageTag: String,
)
