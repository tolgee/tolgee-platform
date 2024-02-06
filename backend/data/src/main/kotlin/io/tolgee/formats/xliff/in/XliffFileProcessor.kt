package io.tolgee.formats.xliff.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.UnsupportedXliffVersionException
import io.tolgee.formats.ios.`in`.AppleXliffFileProcessor
import io.tolgee.formats.xliff.`in`.parser.XliffParser
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import java.util.*
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class XliffFileProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
  override fun process() {
    val parsed = XliffParser(xmlEventReader).parse()
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
    return parsed.files.any {
      it.original?.matches(Regex(".*\\.(?:xc)?strings(?:dict)?$")) == true
    }
  }

  private val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
    inputFactory.createXMLEventReader(context.file.data.inputStream())
  }
}
