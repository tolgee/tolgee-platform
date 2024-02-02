package io.tolgee.formats.xliff.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.UnsupportedXliffVersionException
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import java.util.*
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement

class XliffFileProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
  override fun process() {
    try {
      when (version) {
        "1.2" -> Xliff12FileProcessor(context, xmlEventReader).process()
        else -> throw UnsupportedXliffVersionException(version)
      }
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message)
    }
  }

  private val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
    inputFactory.createXMLEventReader(context.file.data.inputStream())
  }

  private val version: String by lazy {
    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      if (event.isStartElement &&
        (event as? StartElement)?.name?.localPart?.lowercase(Locale.getDefault()) == "xliff"
      ) {
        val versionAttr = event.getAttributeByName(QName(null, "version"))
        if (versionAttr != null) {
          return@lazy versionAttr.value
        }
      }
    }
    throw ImportCannotParseFileException(context.file.name, "No version information")
  }
}
