package io.tolgee.formats.android.out

import io.tolgee.formats.android.`in`.JavaToIcuParamConvertor
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

class TextToAndroidXmlConvertor {
  fun getContent(
    document: Document,
    content: String?,
  ): ContentToAppend? {
    val contentNotNull =
      content ?: let {
        return null
      }

    try {
      val doc = parseString(contentNotNull)
      val analysisResult = doc.analyze()
      if (analysisResult.containsXml && analysisResult.containsPlaceholders) {
        val cdata =
          document.createCDATASection(
            contentNotNull.escape(escapeApos = true, keepPercentSignEscaped = true, quoteMoreWhitespaces = false),
          )
        return ContentToAppend(children = listOf(cdata))
      }

      analysisResult.unsupportedTagNodes.forEach { node ->
        node.parentNode.replaceChild(
          doc.createCDATASection(
            node.writeToString().escape(
              escapeApos = true,
              keepPercentSignEscaped = analysisResult.containsPlaceholders,
              quoteMoreWhitespaces = false,
            ),
          ),
          node,
        )
      }

      analysisResult.textNodes.forEach { node ->
        node.escapeText(keepPercentSignEscaped = analysisResult.containsPlaceholders, quoteMoreWhitespaces = true)
      }

      return ContentToAppend(
        children = doc.childNodes.item(0).childNodes.asSequence().toList(),
      )
    } catch (ex: java.lang.Exception) {
      return ContentToAppend(text = contentNotNull)
    }
  }

  private fun parseString(contentNotNull: String): Document =
    documentBuilder.parse(InputSource(StringReader("<root>$contentNotNull</root>")))

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
        val matches = JavaToIcuParamConvertor.JAVA_PLACEHOLDER_REGEX.findAll(node.textContent)
        if (matches.any { it.value != "%%" }) {
          containsPlaceholders = true
        }
        textNodes.add(node)
      }
      if (node.nodeType == Node.ELEMENT_NODE) {
        if (node.nodeName.lowercase() !in supportedTags) {
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
    )
  }

  private fun Node.isParentRoot(): Boolean {
    return this.parentNode.nodeName == "root" && this.parentNode.parentNode == null
  }

  companion object {
    private val documentBuilder: DocumentBuilder by lazy { DocumentBuilderFactory.newInstance().newDocumentBuilder() }
    private val supportedTags =
      setOf(
        "b", "i", "cite", "dfn", "em",
        "big", "small", "font",
        "tt", "s", "strike", "del", "u",
        "sup", "sub", "ul", "li",
        "br", "div", "p",
      )
    val spacesRegex = """([\u0020\u2008\u2003]{2,})""".toRegex()
    private val xmlTransformer by lazy {
      val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
      transformer
    }
  }

  private fun String.escape(
    escapeApos: Boolean,
    keepPercentSignEscaped: Boolean,
    /**
     * We only support this non XML strings
     */
    quoteMoreWhitespaces: Boolean,
  ): String {
    return this
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .let {
        if (quoteMoreWhitespaces) {
          it.replace(spacesRegex, "\"$1\"")
        } else {
          it
        }
      }
      .let {
        if (!keepPercentSignEscaped) {
          it.replace("%%", "%")
        } else {
          it
        }
      }
      .let {
        if (escapeApos) {
          it.replace("'", "\\'")
        } else {
          it
        }
      }
  }

  data class ContentToAppend(val text: String? = null, val children: Collection<Node>? = null)
}

private data class AnalysisResult(
  val containsXml: Boolean,
  val containsPlaceholders: Boolean,
  val unsupportedTagNodes: List<Node>,
  val textNodes: MutableList<Node>,
)
