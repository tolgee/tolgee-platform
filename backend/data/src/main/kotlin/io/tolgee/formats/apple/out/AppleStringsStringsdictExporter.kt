package io.tolgee.formats.apple.out

import io.tolgee.dtos.IExportParams
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class AppleStringsStringsdictExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  private val isProjectIcuPlaceholdersEnabled: Boolean = true,
  private val stringsFilePathProvider: ExportFilePathProvider,
  private val stringsdictFilePathProvider: ExportFilePathProvider,
) : FileExporter {
  private val preparedFiles = mutableMapOf<String, PreparedFile>()

  override fun produceFiles(): Map<String, InputStream> {
    translations.forEach {
      handleTranslation(it)
    }

    val result = mutableMapOf<String, InputStream>()
    preparedFiles.forEach {
      if (it.value.hasSingle) {
        val path = stringsFilePathProvider.replaceExtensionAndFinalize(it.key)
        result[path] =
          it.value.stringsWriter.result
            .byteInputStream()
      }
      if (it.value.hasPlural) {
        val path = stringsdictFilePathProvider.replaceExtensionAndFinalize(it.key)
        result[path] = it.value.stringsdictWriter.result
      }
    }
    return result
  }

  private fun getBaseFilePath(translation: ExportTranslationView): String {
    return stringsFilePathProvider.getFilePath(
      translation.key.namespace,
      translation.languageTag,
      replaceExtension = false,
    )
  }

  private fun handleTranslation(it: ExportTranslationView) {
    val text = it.text

    if (text == null) {
      handleSingle(it, "")
      return
    }

    val converted =
      IcuToAppleMessageConvertor(message = text, it.key.isPlural, isProjectIcuPlaceholdersEnabled).convert()

    if (converted.isPlural()) {
      handlePlural(it, converted.formsResult ?: return)
      return
    }

    handleSingle(it, converted.singleResult ?: return)
  }

  private fun handlePlural(
    translationView: ExportTranslationView,
    formsResult: Map<String, String>,
  ) {
    val preparedFile = getResultPreparedFile(translationView)
    preparedFile.stringsdictWriter.addEntry(translationView.key.name, formsResult)
  }

  private fun handleSingle(
    rawData: ExportTranslationView,
    convertedText: String,
  ) {
    val preparedFile = getResultPreparedFile(rawData)
    preparedFile.stringsWriter.addEntry(rawData.key.name, convertedText, rawData.key.description)
  }

  private fun getResultPreparedFile(translation: ExportTranslationView): PreparedFile {
    return preparedFiles.getOrPut(getBaseFilePath(translation)) { PreparedFile() }
  }

  class PreparedFile {
    var hasPlural = false
    val stringsdictWriter: StringsdictWriter by lazy {
      hasPlural = true
      StringsdictWriter()
    }

    var hasSingle = false
    val stringsWriter: StringsWriter by lazy {
      hasSingle = true
      StringsWriter()
    }
  }
}
