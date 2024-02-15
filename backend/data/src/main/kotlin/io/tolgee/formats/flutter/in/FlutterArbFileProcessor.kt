package io.tolgee.formats.flutter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.flutter.FLUTTER_ARB_FILE_PLACEHOLDERS_CUSTOM_KEY
import io.tolgee.formats.optimizePossiblePlural
import io.tolgee.service.dataImport.processors.FileProcessorContext

class FlutterArbFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) :
  ImportFileProcessor() {
  override fun process() {
    parsed.translations.forEach { (keyName, item) ->
      context.addTranslation(keyName, guessedLanguage, item.value.convertMessage())
      if (item.description != null) {
        context.addKeyDescription(keyName, item.description)
      }
      if (item.placeholders != null) {
        context.setCustom(keyName, FLUTTER_ARB_FILE_PLACEHOLDERS_CUSTOM_KEY, item.placeholders)
      }
    }
  }

  private val parsed by lazy {
    FlutterArbFileParser(context.file.data, objectMapper).parse()
  }

  private fun String?.convertMessage(): String? {
    this ?: return null
    return optimizePossiblePlural(this)
  }

  private val guessedLanguage: String by lazy {
    parsed.locale ?: guessLanguageFormFileName() ?: "unknown"
  }

  private fun guessLanguageFormFileName(): String? {
    val filename = context.file.name
    val languagePart =
      LANGUAGE_GUESS_REGEX.find(filename)?.let { match ->
        match.groups["language"]?.value
      }

    return languagePart?.replace("_", "-")
  }

  companion object {
    val LANGUAGE_GUESS_REGEX = Regex("app_(?<language>[a-zA-Z_]+).arb")
  }
}
