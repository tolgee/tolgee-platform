package io.tolgee.ee.service.translationMemory.tmx

import java.io.ByteArrayOutputStream
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

// XML 1.0 §2.2 Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF].
// TMX 1.4 mandates XML 1.0, so the underlying stream writer rejects every other codepoint — a
// single user-pasted #x0B vertical tab (Word/Excel copy-paste) blew up the whole export with
// WstxIOException. We walk codepoints rather than UTF-16 chars so valid surrogate pairs (emoji
// etc.) survive intact; lone surrogates and BMP noncharacters are dropped along with the C0
// controls so no other illegal codepoint can sink the file either.
private fun sanitizeXmlText(text: String): String {
  if (text.isEmpty()) return text
  val out = StringBuilder(text.length)
  var i = 0
  while (i < text.length) {
    val cp = text.codePointAt(i)
    if (isValidXmlChar(cp)) out.appendCodePoint(cp)
    i += Character.charCount(cp)
  }
  return out.toString()
}

private fun isValidXmlChar(cp: Int): Boolean {
  if (cp == 0x9 || cp == 0xA || cp == 0xD) return true
  if (cp in 0x20..0xD7FF) return true
  if (cp in 0xE000..0xFFFD) return true
  if (cp in 0x10000..0x10FFFF) return true
  return false
}

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
