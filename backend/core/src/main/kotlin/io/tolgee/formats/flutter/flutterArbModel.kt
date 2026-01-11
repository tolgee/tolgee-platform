package io.tolgee.formats.flutter

class FlutterArbModel(
  val locale: String?,
  val translations: MutableMap<String, FlutterArbTranslationModel> = mutableMapOf(),
)

class FlutterArbTranslationModel(
  val value: String?,
  val description: String? = null,
  val placeholders: Map<String, Any?>? = null,
)
