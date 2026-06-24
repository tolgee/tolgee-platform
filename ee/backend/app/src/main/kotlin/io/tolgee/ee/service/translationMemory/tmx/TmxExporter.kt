package io.tolgee.ee.service.translationMemory.tmx

import io.tolgee.util.sanitizeXmlText
import java.io.ByteArrayOutputStream
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class TmxExporter(
  private val sourceLanguageTag: String,
  private val units: List<TmxExportUnit>,
) {
  fun export(): ByteArray {
    val out = ByteArrayOutputStream()
    val factory = XMLOutputFactory.newInstance()
    val writer = factory.createXMLStreamWriter(out, "UTF-8")

    writer.writeStartDocument("UTF-8", "1.0")
    writer.writeCharacters("\n")
    writer.writeStartElement("tmx")
    writer.writeAttribute("version", "1.4")
    writer.writeCharacters("\n  ")

    writer.writeEmptyElement("header")
    writer.writeAttribute("creationtool", "Tolgee")
    writer.writeAttribute("creationtoolversion", "1.0")
    writer.writeAttribute("datatype", "PlainText")
    writer.writeAttribute("segtype", "sentence")
    writer.writeAttribute("srclang", sourceLanguageTag)
    writer.writeCharacters("\n  ")

    writer.writeStartElement("body")

    var autoTuid = 1
    for (unit in units) {
      writer.writeCharacters("\n    ")
      writer.writeStartElement("tu")
      writer.writeAttribute("tuid", unit.tuid ?: (autoTuid++).toString())

      writeTuv(writer, sourceLanguageTag, unit.sourceText)
      for ((lang, text) in unit.translations) {
        writeTuv(writer, lang, text)
      }

      writer.writeCharacters("\n    ")
      writer.writeEndElement() // tu
    }

    writer.writeCharacters("\n  ")
    writer.writeEndElement() // body
    writer.writeCharacters("\n")
    writer.writeEndElement() // tmx
    writer.writeEndDocument()
    writer.close()

    return out.toByteArray()
  }

  private fun writeTuv(
    writer: XMLStreamWriter,
    lang: String,
    text: String,
  ) {
    writer.writeCharacters("\n      ")
    writer.writeStartElement("tuv")
    // `xml:lang` is a namespaced attribute; the 2-arg writeAttribute(localName, value) overload
    // forbids prefixes in the name and throws XMLStreamException on strict implementations.
    writer.writeAttribute("xml", "http://www.w3.org/XML/1998/namespace", "lang", lang)
    writer.writeStartElement("seg")
    writer.writeCharacters(sanitizeXmlText(text))
    writer.writeEndElement() // seg
    writer.writeEndElement() // tuv
  }
}
