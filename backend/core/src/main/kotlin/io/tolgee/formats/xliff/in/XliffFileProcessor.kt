package io.tolgee.formats.xliff.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.UnsupportedXliffVersionException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.apple.`in`.xliff.AppleXliffFileProcessor
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.xliff.`in`.parser.XliffParser
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.service.dataImport.processors.FileProcessorContext
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class XliffFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  override fun process() {
    val parsed =
      try {
        XliffParser(xmlEventReader).parse()
      } catch (e: ImportCannotParseFileException) {
        throw ImportCannotParseFileException(context.file.name, e.message)
      }
    if (isApple(parsed)) {
      return AppleXliffFileProcessor(context, parsed).process()
    }

    try {
      val version = parsed.version ?: throw ImportCannotParseFileException(context.file.name, "Version not found")
      when (version) {
        "1.2" -> Xliff12FileProcessor(context, parsed).process()
        else -> throw UnsupportedXliffVersionException(version)
      }
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message)
    }
  }

  private fun isApple(parsed: XliffModel): Boolean {
    if (context.mapping?.format == ImportFormat.APPLE_XLIFF) {
      return true
    }

    return parsed.files.any {
      it.original?.matches(Regex(".*\\.(?:xc)?strings(?:dict)?$")) == true
    }
  }

  private val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
    inputFactory.createXMLEventReader(context.file.data.inputStream())
  }
}
