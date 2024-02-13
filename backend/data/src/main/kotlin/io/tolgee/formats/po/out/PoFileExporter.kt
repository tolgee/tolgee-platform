package io.tolgee.formats.po.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.getPluralData
import io.tolgee.formats.po.SupportedFormat
import io.tolgee.model.ILanguage
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class PoFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  baseTranslationsProvider: () -> List<ExportTranslationView>,
  val baseLanguage: ILanguage,
  private val supportedFormat: SupportedFormat,
) : FileExporter {
  override val fileExtension: String = "po"

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
      val converted =
        supportedFormat.exportMessageConverter(
          translation.text!!,
          translation.languageTag,
          translation.key.isPlural,
        ).convert()

      resultBuilder.appendLine()
      resultBuilder.writeMsgId(translation.key.name)
      resultBuilder.writeMsgStr(converted)
    }
  }

  private fun StringBuilder.writeMsgId(keyName: String) {
    this.append(convertToPoMultilineString("msgid", keyName))
  }

  private fun getResultStringBuilder(translation: ExportTranslationView): StringBuilder {
    val path = translation.getFilePath(translation.key.namespace)
    return preparedResult.computeIfAbsent(path) {
      initPoFile(translation)
    }
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
}
