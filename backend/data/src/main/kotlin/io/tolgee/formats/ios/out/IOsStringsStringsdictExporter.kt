package io.tolgee.formats.ios.out

import io.tolgee.dtos.IExportParams
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class IOsStringsStringsdictExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
) : FileExporter {
  override val fileExtension: String = ""

  private val preparedFiles = mutableMapOf<String, PreparedFile>()

  override fun produceFiles(): Map<String, InputStream> {
    translations.forEach {
      handleTranslation(it)
    }

    val result = mutableMapOf<String, InputStream>()
    preparedFiles.forEach {
      if (it.value.hasSingle) {
        result["${it.key}strings"] = it.value.stringsWriter.result.byteInputStream()
      }
      if (it.value.hasPlural) {
        result["${it.key}stringsdict"] = it.value.stringsdictWriter.result
      }
    }
    return result
  }

  private fun getBaseFilePath(translation: ExportTranslationView): String {
    val namespace = translation.key.namespace ?: ""
    val filePath = "${translation.languageTag}.lproj/Localizable."
    return "$namespace/$filePath".replace("^/".toRegex(), "")
  }

  private fun handleTranslation(it: ExportTranslationView) {
    val text = it.text

    if (text == null) {
      handleSingle(it, "")
      return
    }

    val converted = IcuToIOsMessageConvertor(message = text).convert()

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
