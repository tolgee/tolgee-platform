package io.tolgee.service.export.dataProvider

class ExportKeyView(
  var id: Long = 0,
  var name: String,
  var custom: Map<String, Any?>? = null,
  var description: String? = null,
  var namespace: String? = null,
  val translations: MutableMap<String, ExportTranslationView> = mutableMapOf(),
)
