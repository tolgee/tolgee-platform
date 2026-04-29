package io.tolgee.ee.service.translationMemory.tmx

import io.tolgee.model.translationMemory.TranslationMemoryEntry
import java.io.ByteArrayOutputStream
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class TmxExporter(
  private val sourceLanguageTag: String,
  private val entries: List<TranslationMemoryEntry>,
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

    // Header
    writer.writeEmptyElement("header")
    writer.writeAttribute("creationtool", "Tolgee")
    writer.writeAttribute("creationtoolversion", "1.0")
    writer.writeAttribute("datatype", "PlainText")
    writer.writeAttribute("segtype", "sentence")
    writer.writeAttribute("srclang", sourceLanguageTag)
    writer.writeCharacters("\n  ")

    // Body
    writer.writeStartElement("body")

    // Group by tuid if available, otherwise by sourceText
    val grouped = entries.groupBy { it.tuid ?: "auto:${it.sourceText}" }
    var autoTuid = 1
    for ((groupKey, entryGroup) in grouped) {
      writer.writeCharacters("\n    ")
      writer.writeStartElement("tu")
      val tuidValue = entryGroup.firstOrNull()?.tuid ?: (autoTuid++).toString()
      writer.writeAttribute("tuid", tuidValue)
      val sourceText = entryGroup.first().sourceText

      // Source tuv
      writeTuv(writer, sourceLanguageTag, sourceText)

      // Target tuvs
      for (entry in entryGroup) {
        writeTuv(writer, entry.targetLanguageTag, entry.targetText)
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
    writer.writeAttribute("xml:lang", lang)
    writer.writeStartElement("seg")
    writer.writeCharacters(text)
    writer.writeEndElement() // seg
    writer.writeEndElement() // tuv
  }
}
