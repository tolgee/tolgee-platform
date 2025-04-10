package io.tolgee.formats.flutter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.formats.flutter.FlutterArbModel
import io.tolgee.formats.flutter.FlutterArbTranslationModel

class FlutterArbFileParser(
  private val bytes: ByteArray,
  private val objectMapper: ObjectMapper,
) {
  fun parse(): FlutterArbModel {
    val data = objectMapper.readValue<Map<String, Any>>(bytes)
    return parseArbData(data)
  }

  private fun parseArbData(data: Map<String, Any>): FlutterArbModel {
    val locale = data["@@locale"] as? String
    val translations =
      data.entries
        .fold(mutableMapOf<String, FlutterArbTranslationModel>()) { acc, entry ->
          val key = entry.key
          if (!key.startsWith("@@") && !key.startsWith("@")) {
            val value = entry.value as? String ?: ""
            val details = data["@$key"] as? Map<*, *>
            val description = details?.get("description") as? String
            val placeholders = details?.get("placeholders")
            val translationModel =
              FlutterArbTranslationModel(
                value = value,
                description = description,
                placeholders = getSafePlaceHoldersMap(placeholders),
              )
            acc[key] = translationModel
          }
          acc
        }

    return FlutterArbModel(
      locale = locale,
      translations = translations,
    )
  }

  private fun getSafePlaceHoldersMap(placeholders: Any?) =
    (placeholders as? Map<*, *>)
      ?.entries
      ?.mapNotNull { (key, value) ->
        (key as? String ?: return@mapNotNull null) to value
      }?.toMap()
}
