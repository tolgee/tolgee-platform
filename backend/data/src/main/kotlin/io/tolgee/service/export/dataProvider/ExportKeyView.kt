package io.tolgee.service.export.dataProvider

class ExportKeyView(
  val id: Long,
  val name: String,
  val translations: MutableMap<String, ExportTranslationView> = mutableMapOf()
)
