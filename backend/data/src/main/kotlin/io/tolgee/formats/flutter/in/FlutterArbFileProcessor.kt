package io.tolgee.formats.flutter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.flutter.FLUTTER_ARB_FILE_PLACEHOLDERS_CUSTOM_KEY
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class FlutterArbFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  override fun process() {
    parsed.translations.forEach { (keyName, item) ->
      val converted = convertMessage(item.value)
      context.addTranslation(
        keyName,
        guessedLanguage,
        converted.message,
        rawData = item.value,
        convertedBy = ImportFormat.FLUTTER_ARB,
        pluralArgName = converted.pluralArgName,
      )
      if (item.description != null) {
        context.addKeyDescription(keyName, item.description)
      }
      if (item.placeholders != null) {
        context.setCustom(keyName, FLUTTER_ARB_FILE_PLACEHOLDERS_CUSTOM_KEY, item.placeholders)
      }
    }
  }

  private val parsed by lazy {
    try {
      FlutterArbFileParser(context.file.data, objectMapper).parse()
    } catch (e: Exception) {
      throw FlutterArbFileParseException(context.file.name, e)
    }
  }

  fun convertMessage(text: String?): MessageConvertorResult {
    return ImportFormat.FLUTTER_ARB.messageConvertor.convert(
      rawData = text,
      languageTag = guessedLanguage,
      convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
      isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
    )
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
