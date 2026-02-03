package io.tolgee.formats.xmlResources.out

import io.tolgee.formats.ExportFormat
import io.tolgee.formats.MobileStringEscaper
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.xmlResources.XmlResourcesParsingConstants
import io.tolgee.formats.xmlResources.XmlResourcesStringValue
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class TextToXmlResourcesConvertor(
  private val document: Document,
  private val value: XmlResourcesStringValue,
  private val format: ExportFormat,
) : Logging {
  val string = value.string

  fun convert(): ContentToAppend {
    if (value.isWrappedCdata || containsXmlAndPlaceholders) {
      return contentWrappedInCdata
    }

    wrapUnsupportedTagsWithCdata(analysisResult, parsed)
    escapeTextNodes()

    return ContentToAppend(
      children =
        parsed.childNodes
          .item(0)
          .childNodes
          .asSequence()
          .toList(),
    )
  }

  private val parsed by lazy {
    parseString(string)
  }

  private val analysisResult by lazy {
    parsed.analyze()
  }

  private val containsXmlAndPlaceholders
    get() = analysisResult.containsXml && analysisResult.containsPlaceholders

  private val contentWrappedInCdata: ContentToAppend
    get() {
      val cdata =
        document.createCDATASection(
          string.escape(
            escapeApos = true,
            keepPercentSignEscaped = true,
            quoteMoreWhitespaces = false,
            escapeNewLines = true,
          ),
        )
      return ContentToAppend(children = listOf(cdata))
    }

  private fun escapeTextNodes() {
    analysisResult.textNodes.forEach { node ->
      node.escapeText(keepPercentSignEscaped = analysisResult.containsPlaceholders, quoteMoreWhitespaces = true)
    }
  }

  private fun wrapUnsupportedTagsWithCdata(
    analysisResult: AnalysisResult,
    doc: Document,
  ) {
    analysisResult.unsupportedTagNodes.forEach { node ->
      node.parentNode.replaceChild(
        doc.createCDATASection(
          node.writeToString().escape(
            escapeApos = true,
            keepPercentSignEscaped = analysisResult.containsPlaceholders,
            quoteMoreWhitespaces = false,
            escapeNewLines = true,
          ),
        ),
        node,
      )
    }
  }

  private fun parseString(contentNotNull: String): Document {
    try {
      return documentBuilder.parse(InputSource(StringReader("<root>$contentNotNull</root>")))
    } catch (e: Exception) {
      logger.debug("Cannot process XML value '$string'", e)

      // Fallback - all the text as a single text node
      val doc = documentBuilder.newDocument()
      val rootElement = doc.createElement("root")
      doc.appendChild(rootElement)
      rootElement.appendChild(doc.createTextNode(contentNotNull))
      return doc
    }
  }

  private fun Node.writeToString(): String {
    val source = DOMSource(this)
    val writer = StringWriter()
    val result = StreamResult(writer)
    xmlTransformer.transform(source, result)
    return writer.buffer.toString()
  }

  private fun NodeList.forEach(action: (Node) -> Unit) {
    this.asSequence().forEach(action)
  }

  private fun NodeList.asSequence(): Sequence<Node> {
    return (0 until this.length).asSequence().map { this.item(it) }
  }

  private fun Document.analyze(): AnalysisResult {
    var containsTags = false
    var containsPlaceholders = false
    val unsupportedTagNodes = mutableListOf<Node>()
    val textNodes = mutableListOf<Node>()
    forEachNodeDeep { node ->
      if (node.nodeType == Node.TEXT_NODE) {
        val matches = JavaToIcuPlaceholderConvertor.JAVA_PLACEHOLDER_REGEX.findAll(node.textContent)
        if (matches.any { it.value != "%%" }) {
          containsPlaceholders = true
        }
        textNodes.add(node)
      }
      if (node.nodeType == Node.ELEMENT_NODE) {
        if (!isAndroid || node.nodeName.lowercase() !in XmlResourcesParsingConstants.androidSupportedTags) {
          unsupportedTagNodes.add(node)
        } else {
          containsTags = true
        }
      }
    }
    return AnalysisResult(
      containsXml = containsTags,
      containsPlaceholders = containsPlaceholders,
      unsupportedTagNodes = unsupportedTagNodes,
      textNodes = textNodes,
    )
  }

  private fun Document.forEachNodeDeep(action: (Node) -> Unit) {
    val childNodes = this.documentElement.childNodes
    for (i in 0 until childNodes.length) {
      val node = childNodes.item(i)
      action(node)
      if (node.hasChildNodes()) {
        node.childNodes.forEach {
          action(it)
        }
      }
    }
  }

  private fun Node.escapeText(
    keepPercentSignEscaped: Boolean,
    quoteMoreWhitespaces: Boolean,
  ) {
    this.textContent = this.getEscapedText(keepPercentSignEscaped, quoteMoreWhitespaces)
  }

  private fun Node.getEscapedText(
    keepPercentSignEscaped: Boolean,
    quoteMoreWhitespaces: Boolean,
  ): String {
    return this.textContent.escape(
      escapeApos = isParentRoot(),
      keepPercentSignEscaped = keepPercentSignEscaped,
      quoteMoreWhitespaces = quoteMoreWhitespaces,
      escapeNewLines = !analysisResult.containsXml,
    )
  }

  private fun Node.isParentRoot(): Boolean {
    return this.parentNode.nodeName == "root" && this.parentNode.parentNode === this.ownerDocument
  }

  private val documentBuilder: DocumentBuilder by lazy { documentBuilderFactory.newDocumentBuilder() }

  private val xmlTransformer: Transformer by lazy {
    val transformer: Transformer = xmlTransformerFactory.newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer
  }

  companion object {
    private val documentBuilderFactory: DocumentBuilderFactory by lazy { DocumentBuilderFactory.newInstance() }

    private val xmlTransformerFactory by lazy { TransformerFactory.newInstance() }
  }

  private fun String.escape(
    escapeApos: Boolean,
    keepPercentSignEscaped: Boolean,
    /**
     * We only support this non XML strings
     */
    quoteMoreWhitespaces: Boolean,
    escapeNewLines: Boolean,
  ): String {
    return MobileStringEscaper(
      string = this,
      escapeApos = isAndroid && escapeApos,
      keepPercentSignEscaped = keepPercentSignEscaped,
      quoteMoreWhitespaces = isAndroid && quoteMoreWhitespaces,
      escapeNewLines = isAndroid && escapeNewLines,
      utfSymbolCharacter = 'u',
      escapeQuotes = isAndroid,
    ).escape()
  }

  val isAndroid
    get() = format == ExportFormat.ANDROID_XML

  data class ContentToAppend(
    val text: String? = null,
    val children: Collection<Node>? = null,
  )

  private data class AnalysisResult(
    val containsXml: Boolean,
    val containsPlaceholders: Boolean,
    val unsupportedTagNodes: List<Node>,
    val textNodes: MutableList<Node>,
  )
}
