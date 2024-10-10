package io.tolgee.service.export.dataProvider

import io.tolgee.model.enums.TranslationState

class ExportTranslationView(
  val id: Long?,
  val text: String?,
  val state: TranslationState = TranslationState.TRANSLATED,
  val key: ExportKeyView,
  val languageTag: String = "en",
  val description: String? = null,
)
