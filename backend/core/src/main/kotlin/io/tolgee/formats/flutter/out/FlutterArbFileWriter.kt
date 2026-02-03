package io.tolgee.formats.flutter.out

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.formats.flutter.FlutterArbModel
import io.tolgee.formats.flutter.FlutterArbTranslationModel
import java.io.InputStream

class FlutterArbFileWriter(
  val model: FlutterArbModel,
  private val objectMapper: ObjectMapper,
) {
  val result = mutableMapOf<String, Any?>()

  fun produceFile(): InputStream {
    addLocale()
    addTranslations()
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(result).inputStream()
  }

  private fun addTranslations() {
    model.translations.forEach {
      addTranslation(it)
    }
  }

  private fun addTranslation(it: Map.Entry<String, FlutterArbTranslationModel>) {
    result[it.key] = it.value.value
    addMeta(it)
  }

  private fun addMeta(it: Map.Entry<String, FlutterArbTranslationModel>) {
    if (it.value.description == null && it.value.placeholders == null) {
      return
    }
    val meta = mutableMapOf<String, Any>()
    result["@${it.key}"] = meta
    it.value.description?.let { meta["description"] = it }
    it.value.placeholders?.let { meta["placeholders"] = it }
  }

  private fun addLocale() {
    val locale = model.locale ?: return
    result["@@locale"] = locale
  }
}
