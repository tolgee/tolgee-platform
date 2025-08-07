package io.tolgee.formats.apple.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.tolgee.dtos.IExportParams
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class AppleXcstringsExporter(
  private val translations: List<ExportTranslationView>,
  private val exportParams: IExportParams,
  private val objectMapper: ObjectMapper,
  private val isProjectIcuPlaceholdersEnabled: Boolean = true,
  private val filePathProvider: ExportFilePathProvider,
) : FileExporter {
  private val preparedFiles = mutableMapOf<String, ObjectNode>()

  override fun produceFiles(): Map<String, InputStream> {
    translations.forEach { handleTranslation(it) }

    return preparedFiles.mapValues { (_, jsonContent) ->
      val root =
        objectMapper.createObjectNode().apply {
          put("sourceLanguage", exportParams.languages?.firstOrNull() ?: "en")
          put("version", "1.0")
          set<ObjectNode>("strings", jsonContent)
        }
      objectMapper.writeValueAsString(root).byteInputStream()
    }
  }

  private fun handleTranslation(translation: ExportTranslationView) {
    val baseFilePath = getBaseFilePath(translation)
    val fileContent = preparedFiles.getOrPut(baseFilePath) { objectMapper.createObjectNode() }

    val keyData =
      fileContent.get(translation.key.name)?.let {
        it as ObjectNode
      } ?: createKeyEntry(translation)
    fileContent.set<ObjectNode>(translation.key.name, keyData)

    val converted =
      IcuToAppleMessageConvertor(
        message = translation.text ?: "",
        translation.key.isPlural,
        isProjectIcuPlaceholdersEnabled,
      ).convert()

    val localizations =
      keyData.get("localizations")?.let {
        it as ObjectNode
      } ?: objectMapper.createObjectNode()

    if (translation.description != null) {
      keyData.put("comment", translation.description)
    }

    if (converted.isPlural()) {
      handlePluralTranslation(localizations, translation, converted.formsResult)
    } else {
      handleSingleTranslation(localizations, translation, converted.singleResult)
    }

    keyData.set<ObjectNode>("localizations", localizations)
  }

  private fun getAppleState(state: TranslationState): String? {
    return when (state) {
      TranslationState.TRANSLATED -> "needs_review"
      TranslationState.REVIEWED -> "translated"
      TranslationState.UNTRANSLATED -> null
      TranslationState.DISABLED -> null
    }
  }

  private fun handleSingleTranslation(
    localizations: ObjectNode,
    translation: ExportTranslationView,
    convertedText: String?,
  ) {
    if (convertedText == null) return

    localizations.set<ObjectNode>(
      translation.languageTag,
      objectMapper.createObjectNode().apply {
        set<ObjectNode>(
          "stringUnit",
          objectMapper.createObjectNode().apply {
            getAppleState(translation.state)?.let { state ->
              put("state", state)
            }
            put("value", convertedText)
          },
        )
      },
    )
  }

  private fun handlePluralTranslation(
    localizations: ObjectNode,
    translation: ExportTranslationView,
    forms: Map<String, String>?,
  ) {
    if (forms == null) return

    val pluralForms = objectMapper.createObjectNode()
    forms.forEach { (form, text) ->
      pluralForms.set<ObjectNode>(
        form,
        objectMapper.createObjectNode().apply {
          set<ObjectNode>(
            "stringUnit",
            objectMapper.createObjectNode().apply {
              getAppleState(translation.state)?.let { state ->
                put("state", state)
              }
              put("value", text)
            },
          )
        },
      )
    }

    localizations.set<ObjectNode>(
      translation.languageTag,
      objectMapper.createObjectNode().apply {
        set<ObjectNode>(
          "variations",
          objectMapper.createObjectNode().apply {
            set<ObjectNode>("plural", pluralForms)
          },
        )
      },
    )
  }

  private fun createKeyEntry(translation: ExportTranslationView): ObjectNode {
    return objectMapper.createObjectNode().apply {
      translation.key.description?.let {
        put("extractionState", "manual")
      }
    }
  }

  private fun getBaseFilePath(translation: ExportTranslationView): String {
    return filePathProvider.getFilePath(
      translation.key.namespace,
      null,
      replaceExtension = true,
    )
  }
}
