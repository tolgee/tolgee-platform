package io.tolgee.formats.xliff.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.helpers.TextHelper
import io.tolgee.model.ILanguage
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream

class XliffFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  baseTranslationsProvider: () -> List<ExportTranslationView>,
  val baseLanguage: ILanguage,
) : FileExporter {
  override val fileExtension: String = ExportFormat.XLIFF.extension

  val result = mutableMapOf<String, ResultItem>()
  private val baseTranslations: Map<String, ExportTranslationView>

  init {
    this.baseTranslations = baseTranslationsProvider().associateBy { it.key.name }
  }

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return result.asSequence().map { (fileName, resultItem) ->
      val outputStream = ByteArrayOutputStream()
      val writer = XMLWriter(outputStream, OutputFormat.createPrettyPrint())
      writer.write(resultItem.document)
      fileName to outputStream.toByteArray().inputStream()
    }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val path = TextHelper.splitOnNonEscapedDelimiter(translation.key.name, exportParams.structureDelimiter)
      val resultItem = getResultItem(translation)
      addToFileElement(resultItem.fileBodyElement, path, translation)
    }
  }

  private fun addToFileElement(
    fileBodyElement: Element,
    pathItems: List<String>,
    translation: ExportTranslationView,
  ) {
    val transUnitElement =
      fileBodyElement.addElement("trans-unit")
        .addAttribute("id", pathItems.joinToString(exportParams.structureDelimiter.toString()))
        .addAttribute("datatype", "html")

    baseTranslations[translation.key.name]?.text?.let {
      transUnitElement.addElement("source").addFromHtmlOrText(it)
    }
    translation.text?.let {
      transUnitElement.addElement("target").addFromHtmlOrText(it)
    }
  }

  private fun getResultItem(translation: ExportTranslationView): ResultItem {
    val absolutePath = translation.getFilePath(translation.key.namespace)
    return result[absolutePath] ?: let {
      val resultItem = createBaseDocumentStructure(translation)
      result[absolutePath] = resultItem
      return resultItem
    }
  }

  private fun createBaseDocumentStructure(translation: ExportTranslationView): ResultItem {
    val document = DocumentHelper.createDocument()
    document.xmlEncoding = "UTF-8"
    val fileBodyElement =
      document.addElement("xliff")
        .addNamespace("", "urn:oasis:names:tc:xliff:document:1.2")
        .addAttribute("version", "1.2")
        .addElement("file", "urn:oasis:names:tc:xliff:document:1.2")
        .addAttribute("original", "undefined")
        .addAttribute("datatype", "plaintext")
        .addAttribute("source-language", baseLanguage.tag)
        .addAttribute("target-language", translation.languageTag)
        .addElement("body")
    return ResultItem(document, fileBodyElement)
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

  data class ResultItem(val document: Document, val fileBodyElement: Element)
}
