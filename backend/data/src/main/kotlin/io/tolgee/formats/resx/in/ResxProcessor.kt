package io.tolgee.formats.resx.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.resx.ResxEntry
import io.tolgee.service.dataImport.processors.FileProcessorContext
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class ResxProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
  override fun process() {
    val data = parse()
    data.importAll()
  }

  fun Sequence<ResxEntry>.importAll() {
    forEachIndexed { idx, it -> it.import(idx) }
  }

  fun ResxEntry.import(index: Int) {
    val converted =
      messageConvertor.convert(
        data,
        firstLanguageTagGuessOrUnknown,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
      )
    context.addKeyDescription(key, comment)
    context.addTranslation(
      key,
      firstLanguageTagGuessOrUnknown,
      converted.message,
      index,
      pluralArgName = converted.pluralArgName,
      rawData = data,
      convertedBy = importFormat,
    )
  }

  private fun parse(): Sequence<ResxEntry> {
    try {
      val parser = ResxParser(xmlEventReader)
      return parser.parse()
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "", e)
    }
  }

  private val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newInstance()
    inputFactory.createXMLEventReader(context.file.data.inputStream())
  }

  companion object {
    private val importFormat = ImportFormat.RESX_ICU

    private val messageConvertor = importFormat.messageConvertor
  }
}
