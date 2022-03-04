package io.tolgee.service.export.dataProvider

import io.tolgee.model.enums.TranslationState

data class ExportDataView(
  val keyId: Long,
  val keyName: String,
  val languageId: Long,
  val languageTag: String,
  val translationId: Long?,
  val translationText: String?,
  val translationState: TranslationState?
)
