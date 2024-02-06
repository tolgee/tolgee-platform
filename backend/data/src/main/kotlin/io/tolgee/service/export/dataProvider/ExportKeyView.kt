package io.tolgee.service.export.dataProvider

class ExportKeyView(
  val id: Long,
  val name: String,
  val custom: Map<String, Any?>? = null,
  val description: String? = null,
  val namespace: String? = null,
  val translations: MutableMap<String, ExportTranslationView> = mutableMapOf(),
)
