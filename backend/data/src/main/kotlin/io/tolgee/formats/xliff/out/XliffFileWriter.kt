package io.tolgee.formats.xliff.out

import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream

class XliffFileWriter(
  private val xliffModel: XliffModel,
  private val enableHtml: Boolean,
) {
  private lateinit var document: Document
  private lateinit var xliffElement: Element

  fun produceFiles(): InputStream {
    xliffElement = createBaseDocumentStructure()

    xliffModel.files.forEach { xliffFile ->
      val file = createFileBody(xliffFile)
      xliffFile.transUnits.forEach { transUnit ->
        addToFileElement(file, transUnit)
      }
    }

    val outputStream = ByteArrayOutputStream()
    val writer = XMLWriter(outputStream, OutputFormat.createPrettyPrint())
    writer.write(document)
    return outputStream.toByteArray().inputStream()
  }

  private fun addToFileElement(
    fileBodyElement: Element,
    transUnit: XliffTransUnit,
  ) {
    val transUnitElement =
      fileBodyElement.addElement("trans-unit")
        .addAttribute("id", transUnit.id)?.also {
          if (enableHtml) {
            it.addAttribute("datatype", "html")
          }
        }!!

    transUnit.source?.let {
      transUnitElement.addElement("source").addFromHtmlOrText(it)
    }

    transUnit.target?.let {
      transUnitElement.addElement("target").addFromHtmlOrText(it)
    }
  }

  private fun createBaseDocumentStructure(): Element {
    document = DocumentHelper.createDocument()
    document.xmlEncoding = "UTF-8"
    return document.addElement("xliff")
      .addNamespace("", "urn:oasis:names:tc:xliff:document:1.2")
      .addAttribute("version", "1.2")
  }

  private fun createFileBody(file: XliffFile): Element {
    return xliffElement.addElement("file", "urn:oasis:names:tc:xliff:document:1.2")
      .addAttribute("original", file.original ?: "")
      .addAttribute("datatype", "plaintext")
      .addAttribute("source-language", file.sourceLanguage)
      .addAttribute("target-language", file.targetLanguage)
      .addElement("body")
  }

  private fun String.parseHtml(): MutableIterator<Any?> {
    val fragment =
      DocumentHelper
        .parseText("<root>$this</root>")
    return fragment.rootElement.nodeIterator()
  }

  /**
   * For string containing something, which is not parseable as xml such as
   * "Value has to be < 1"
   * It just appends text.
   */
  private fun Element.addFromHtmlOrText(string: String) {
    if (!enableHtml) {
      this.addText(string)
      return
    }
    try {
      string.parseHtml().forEach { node ->
        if (node !is Node) return@forEach
        node.parent = null
        this.add(node)
      }
    } catch (e: DocumentException) {
      this.addText(string)
    }
  }
}
