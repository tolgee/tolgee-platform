package io.tolgee.service.export.dataProvider

class ExportKeyView(
  val id: Long,
  val name: String,
  val namespace: String? = null,
  val translations: MutableMap<String, ExportTranslationView> = mutableMapOf(),
)
