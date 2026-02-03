package io.tolgee.formats.apple.`in`.xcstrings

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.apple.`in`.guessNamespaceFromPath
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class XcstringsFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  private lateinit var sourceLanguage: String

  override fun process() {
    try {
      val root = objectMapper.readTree(context.file.data.inputStream())
      sourceLanguage = root.get("sourceLanguage")?.asText()
        ?: throw ImportCannotParseFileException(context.file.name, "Missing sourceLanguage in xcstrings file")

      val strings =
        root.get("strings")
          ?: throw ImportCannotParseFileException(context.file.name, "Missing 'strings' object in xcstrings file")

      strings.fields().forEach { (key, value) ->
        processKey(key, value)
      }

      context.namespace = guessNamespaceFromPath(context.file.name)
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message)
    }
  }

  private fun addConvertedTranslation(
    key: String,
    languageTag: String,
    rawValue: String,
    forms: Map<String, String>? = null,
  ) {
    val converted =
      messageConvertor.convert(
        rawData = if (forms != null) forms else rawValue,
        languageTag = languageTag,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
      )

    context.addTranslation(
      keyName = key,
      languageName = languageTag,
      value = converted.message,
      convertedBy = importFormat,
      rawData = forms ?: rawValue,
      pluralArgName = converted.pluralArgName,
    )
  }

  private fun processKey(
    key: String,
    value: JsonNode,
  ) {
    val localizations = value.get("localizations") ?: return

    value.get("comment")?.asText()?.let { comment ->
      context.addKeyDescription(key, comment)
    }

    if (!localizations.has(sourceLanguage)) {
      addConvertedTranslation(key, sourceLanguage, key)
    }

    localizations.fields().forEach { (languageTag, localization) ->
      when {
        localization.has("stringUnit") -> {
          processSingleTranslation(key, languageTag, localization)
        }

        localization.has("variations") -> {
          processPluralTranslation(key, languageTag, localization)
        }
      }
    }
  }

  private fun processSingleTranslation(
    key: String,
    languageTag: String,
    localization: JsonNode,
  ) {
    val stringUnit = localization.get("stringUnit")
    stringUnit?.get("state")?.asText()

    val translationValue = stringUnit?.get("value")?.asText()

    if (translationValue != null) {
      addConvertedTranslation(key, languageTag, translationValue)
      return
    }
  }

  private fun processPluralTranslation(
    key: String,
    languageTag: String,
    localization: JsonNode,
  ) {
    val variations = localization.get("variations")?.get("plural") ?: return
    val forms = mutableMapOf<String, String>()

    variations.fields().forEach { (form, content) ->
      val stringUnit = content.get("stringUnit")
      val value = stringUnit?.get("value")?.asText()

      if (value != null) {
        forms[form] = value
      }
    }

    if (forms.isNotEmpty()) {
      addConvertedTranslation(key, languageTag, "", forms)
    }
  }

  companion object {
    private val importFormat = ImportFormat.APPLE_XCSTRINGS
    private val messageConvertor = importFormat.messageConvertor
  }
}
