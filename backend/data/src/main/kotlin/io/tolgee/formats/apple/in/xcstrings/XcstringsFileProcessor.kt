package io.tolgee.formats.apple.`in`.xcstrings

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.apple.`in`.guessNamespaceFromPath
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.model.enums.TranslationState

class XcstringsFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  override fun process() {
    try {
      val root = objectMapper.readTree(context.file.data.inputStream())
      val strings =
        root.get("strings") ?: throw ImportCannotParseFileException(
          context.file.name,
          "Missing 'strings' object in xcstrings file",
        )

      strings.fields().forEach { (key, value) ->
        processKey(key, value)
      }

      context.namespace = guessNamespaceFromPath(context.file.name)
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message)
    }
  }

  private fun processKey(
    key: String,
    value: JsonNode,
  ) {
    val localizations = value.get("localizations") ?: return

    value.get("comment")?.asText()?.let { comment ->
      context.addKeyDescription(key, comment)
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


  private fun convertXCtoTolgeeTranslationState(xcState: String?): TranslationState {
    return when (xcState) {
      "needs_review" -> TranslationState.TRANSLATED
      "translated" -> TranslationState.REVIEWED
      null -> TranslationState.UNTRANSLATED
      else -> TranslationState.UNTRANSLATED
    }
  }

  private fun processSingleTranslation(
    key: String,
    languageTag: String,
    localization: JsonNode,
  ) {
    val stringUnit = localization.get("stringUnit")
    val xcState = stringUnit?.get("state")?.asText()
    var translationState = convertXCtoTolgeeTranslationState(xcState)
    
    val translationValue = stringUnit?.get("value")?.asText()

    if (translationValue != null) {
      context.addTranslation(
        keyName = key,
        languageName = languageTag,
        value = translationValue,
        convertedBy = importFormat,
        state = translationState
      )
    }
  }

  private fun processPluralTranslation(
    key: String,
    languageTag: String,
    localization: JsonNode,
  ) {
    val variations = localization.get("variations")?.get("plural") ?: return
    val forms = mutableMapOf<String, String>()
    var translationState = TranslationState.UNTRANSLATED

    variations.fields().forEach { (form, content) ->
      val stringUnit = content.get("stringUnit")
      val value = stringUnit?.get("value")?.asText()
      val currentState = convertXCtoTolgeeTranslationState(stringUnit?.get("state")?.asText())

      if (form == "other" || form == "zero" || translationState == TranslationState.UNTRANSLATED) {
        translationState = currentState
      }
      if (value != null) {
        forms[form] = value
      }
    }

    if (forms.isNotEmpty()) {
      val converted =
        messageConvertor.convert(
          forms,
          languageTag,
          context.importSettings.convertPlaceholdersToIcu,
          context.projectIcuPlaceholdersEnabled,
        )

      context.addTranslation(
        keyName = key,
        languageName = languageTag,
        value = converted.message,
        pluralArgName = converted.pluralArgName,
        rawData = forms,
        convertedBy = importFormat,
        state = translationState
      )
    }
  }

  companion object {
    private val importFormat = ImportFormat.APPLE_XCSTRINGS
    private val messageConvertor = importFormat.messageConvertor
  }
}
