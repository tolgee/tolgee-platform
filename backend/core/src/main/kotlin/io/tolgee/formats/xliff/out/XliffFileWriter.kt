package io.tolgee.formats.xliff.out

import io.tolgee.formats.MobileStringEscaper
import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
import io.tolgee.util.appendXmlOrText
import io.tolgee.util.attr
import io.tolgee.util.buildDom
import io.tolgee.util.element
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream

class XliffFileWriter(
  private val xliffModel: XliffModel,
  private val enableXmlContent: Boolean,
) {
  private lateinit var xliffElement: Element

  fun produceFiles(): InputStream {
    return buildDom {
      xliffElement = createBaseDocumentStructure()

      for (xliffFile in xliffModel.files) {
        val file = createFileBody(xliffFile)
        for (transUnit in xliffFile.transUnits) {
          file.addToElement(transUnit)
        }
      }
    }.write().toByteArray().inputStream()
  }

  private fun Document.createBaseDocumentStructure(): Element {
    return element("xliff") {
      attr("version", "1.2")
      attr("xmlns", "urn:oasis:names:tc:xliff:document:1.2")
    }
  }

  private fun createFileBody(file: XliffFile): Element {
    return xliffElement.element("file") {
      element("header") {
        element("tool") {
          attr("tool-id", "tolgee.io")
          attr("tool-name", "Tolgee")
        }
      }
      attr("original", file.original ?: "")
      attr("datatype", file.datatype)
      file.sourceLanguage?.let { attr("source-language", it) }
      file.targetLanguage?.let { attr("target-language", it) }
      return element("body")
    }
  }

  private fun Element.addToElement(transUnit: XliffTransUnit) {
    element("trans-unit") {
      attr("id", transUnit.id)

      element("source") {
        attr("xml:space", "preserve")
        appendXmlIfEnabledOrText(transUnit.source?.escaped())
      }

      if (transUnit.target != null) {
        element("target") {
          attr("xml:space", "preserve")
          appendXmlIfEnabledOrText(transUnit.target?.escaped())
        }
      }

      if (transUnit.note != null) {
        element("note") {
          attr("xml:space", "preserve")
          appendXmlIfEnabledOrText(transUnit.note)
        }
      }
    }
  }

  private fun String.escaped(): String {
    return MobileStringEscaper(
      string = this,
      escapeApos = false,
      keepPercentSignEscaped = true,
      quoteMoreWhitespaces = false,
      escapeNewLines = false,
      escapeQuotes = false,
      utfSymbolCharacter = 'U',
    ).escape()
  }

  private fun Element.appendXmlIfEnabledOrText(content: String?) {
    if (!enableXmlContent) {
      textContent = content
      return
    }
    this.appendXmlOrText(content)
  }
}
