package io.tolgee.formats.flutter.out

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.DEFAULT_PLURAL_ARGUMENT_NAME
import io.tolgee.formats.flutter.FLUTTER_ARB_FILE_PLACEHOLDERS_CUSTOM_KEY
import io.tolgee.formats.flutter.FlutterArbModel
import io.tolgee.formats.flutter.FlutterArbTranslationModel
import io.tolgee.formats.toIcuPluralString
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class FlutterArbFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  private val baseLanguageTag: String,
  private val objectMapper: ObjectMapper,
  private val isProjectIcuPlaceholdersEnabled: Boolean = true,
) : FileExporter {
  /**
   * Map (Path To file -> Map (Key Name -> Node Wrapper))
   */
  private val fileUnits = mutableMapOf<String, FlutterArbModel>()

  private fun getModels(): Map<String, FlutterArbModel> {
    prepare()

    return fileUnits
  }

  private fun prepare() {
    translations.forEach { translation ->
      if (translation.languageTag == baseLanguageTag) {
        handleBaseTranslation(translation)
        return@forEach
      }
      handleNonBaseTranslation(translation)
    }
  }

  private fun handleBaseTranslation(translation: ExportTranslationView) {
    getModel(translation).translations[translation.key.name] =
      FlutterArbTranslationModel(
        value = getConvertedMessage(translation),
        description = translation.key.description,
        placeholders = getPlaceholders(translation),
      )
  }

  private fun handleNonBaseTranslation(translation: ExportTranslationView) {
    getModel(translation).translations[translation.key.name] =
      FlutterArbTranslationModel(
        value = getConvertedMessage(translation),
      )
  }

  private fun getPlaceholders(translation: ExportTranslationView): Map<String, Any?>? {
    val possibleMap = translation.key.custom?.get(FLUTTER_ARB_FILE_PLACEHOLDERS_CUSTOM_KEY) as? Map<*, *>
    return possibleMap?.mapNotNull { (key, value) ->
      if (key !is String) return@mapNotNull null
      key to value
    }?.toMap()
  }

  private fun getModel(translation: ExportTranslationView): FlutterArbModel {
    val path = getFilePath(translation.languageTag)
    return fileUnits.computeIfAbsent(path) {
      FlutterArbModel(
        locale = convertLanguageTag(translation.languageTag),
      )
    }
  }

  private fun getFilePath(languageTag: String): String {
    val tagPart = convertLanguageTag(languageTag)
    return "app_$tagPart.$fileExtension"
  }

  private fun convertLanguageTag(languageTag: String) = languageTag.replace("-", "_")

  private fun getConvertedMessage(
    translation: ExportTranslationView,
    isPlural: Boolean = translation.key.isPlural,
  ): String? {
    translation.text ?: return null
    val converted =
      IcuToFlutterArbMessageConvertor(
        message = translation.text ?: "",
        forceIsPlural = isPlural,
        isProjectIcuPlaceholdersEnabled,
      ).convert()

    if (converted.isPlural()) {
      return converted.formsResult!!.toIcuPluralString(
        addNewLines = false,
        argName = converted.argName ?: converted.firstArgName ?: DEFAULT_PLURAL_ARGUMENT_NAME,
      )
    }

    return converted.singleResult!!
  }

  override val fileExtension: String
    get() = "arb"

  override fun produceFiles(): Map<String, InputStream> {
    return getModels().map { (path, model) ->
      path to FlutterArbFileWriter(model, objectMapper).produceFile()
    }.toMap()
  }
}
