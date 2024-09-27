package io.tolgee.formats.po.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.getPluralData
import io.tolgee.formats.po.PO_FILE_MSG_ID_PLURAL_CUSTOM_KEY
import io.tolgee.model.ILanguage
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class PoFileExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  baseTranslationsProvider: () -> List<ExportTranslationView>,
  val baseLanguage: ILanguage,
  private val projectIcuPlaceholdersSupport: Boolean = true,
) : FileExporter {
  val fileExtension: String = "po"

  private val preparedResult: LinkedHashMap<String, StringBuilder> = LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    prepareResult()
    return preparedResult.asSequence().map { (fileName, content) ->
      fileName to content.toString().byteInputStream()
    }.toMap()
  }

  private fun prepareResult() {
    translations.forEach { translation ->
      val resultBuilder = getResultStringBuilder(translation)
      val converted = convertMessage(translation)
      resultBuilder.appendLine()
      resultBuilder.writeMsgId(translation.key.name)
      resultBuilder.writeMsgIdPlural(translation, converted)
      resultBuilder.writeMsgStr(converted)
    }
  }

  private fun convertMessage(translation: ExportTranslationView): ToPoConversionResult {
    return IcuToPoMessageConvertor(
      message = translation.text ?: "",
      languageTag = translation.languageTag,
      placeholderConvertor = messageFormat.paramConvertorFactory(),
      forceIsPlural = translation.key.isPlural,
      projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
    ).convert()
  }

  private fun StringBuilder.writeMsgId(keyName: String) {
    this.append(convertToPoMultilineString("msgid", keyName))
  }

  private val messageFormat by lazy {
    exportParams.messageFormat ?: ExportMessageFormat.ICU
  }

  private fun getResultStringBuilder(translation: ExportTranslationView): StringBuilder {
    val path = filePathProvider.getFilePath(translation)
    return preparedResult.computeIfAbsent(path) {
      initPoFile(translation)
    }
  }

  private val filePathProvider by lazy {
    ExportFilePathProvider(
      exportParams,
      fileExtension,
    )
  }

  private fun initPoFile(translation: ExportTranslationView): StringBuilder {
    val builder = StringBuilder()
    val pluralData = getPluralData(translation.languageTag)
    builder.appendLine("msgid \"\"")
    builder.appendLine("msgstr \"\"")
    builder.appendLine("\"Language: ${translation.languageTag}\\n\"")
    builder.appendLine("\"MIME-Version: 1.0\\n\"")
    builder.appendLine("\"Content-Type: text/plain; charset=UTF-8\\n\"")
    builder.appendLine("\"Content-Transfer-Encoding: 8bit\\n\"")
    builder.appendLine("\"Plural-Forms: ${pluralData.pluralsText}\\n\"")
    builder.appendLine("\"X-Generator: Tolgee\\n\"")
    return builder
  }

  private fun StringBuilder.writeMsgStr(converted: ToPoConversionResult) {
    if (converted.isPlural()) {
      writePlural(converted.formsResult)
      return
    }

    writeSingle(converted.singleResult)
  }

  private fun StringBuilder.writePlural(forms: List<String>?) {
    forms?.forEachIndexed { index, form ->
      this.append(convertToPoMultilineString("msgstr[$index]", form))
    }
  }

  private fun StringBuilder.writeSingle(result: String?) {
    this.append(convertToPoMultilineString("msgstr", result ?: ""))
  }

  private fun StringBuilder.writeMsgIdPlural(
    translation: ExportTranslationView,
    converted: ToPoConversionResult,
  ) {
    val msgIdPlural = translation.key.custom?.get(PO_FILE_MSG_ID_PLURAL_CUSTOM_KEY) as? String ?: return
    if (converted.isPlural()) {
      this.append(
        convertToPoMultilineString(
          "msgid_plural",
          msgIdPlural,
        ),
      )
    }
  }
}
