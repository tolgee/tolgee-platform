package io.tolgee.util

import org.w3c.dom.Comment
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun buildDom(builder: Document.() -> Unit): DomBuilder {
  val documentBuilderFactory = DocumentBuilderFactory.newInstance()
  val documentBuilder = documentBuilderFactory.newDocumentBuilder()
  val document = documentBuilder.newDocument()

  val domBuilder = DomBuilder(document)
  builder(domBuilder.document)
  return domBuilder
}

class DomBuilder(
  val document: Document,
) {
  companion object {
    private val xmlTransformer by lazy {
      val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
      transformer.setOutputProperty(OutputKeys.INDENT, "yes")
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
      transformer
    }
  }

  fun write(): String {
    val source = DOMSource(document)
    val writer = StringWriter()
    val result = StreamResult(writer)
    xmlTransformer.transform(source, result)
    return writer.buffer.toString()
  }
}

inline fun Document.element(
  name: String,
  builder: Element.() -> Unit = {},
): Element {
  val element = this.createElement(name)
  this.appendChild(element)
  builder(element)
  return element
}

fun Element.comment(comment: String): Comment {
  val commentNode = this.ownerDocument.createComment(comment)
  this.appendChild(commentNode)
  return commentNode
}

inline fun Element.element(
  name: String,
  builder: (Element.() -> Unit) = {},
): Element {
  val element = this.ownerDocument.createElement(name)
  this.appendChild(element)
  builder.invoke(element)
  return element
}

fun Element.attr(
  name: String,
  value: String?,
) {
  val attr = this.ownerDocument.createAttribute(name)
  attr.value = value ?: ""
  this.setAttributeNode(attr)
}

fun Element.appendXmlOrText(content: String?) {
  val contentNotNull = content ?: ""
  try {
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc: Document = documentBuilder.parse(InputSource(StringReader("<root>$contentNotNull</root>")))
    val childNodes: NodeList = doc.documentElement.childNodes
    for (i in 0 until childNodes.length) {
      val importedNode: Node = this.ownerDocument.importNode(childNodes.item(i), true)
      appendChild(importedNode)
    }
  } catch (ex: java.lang.Exception) {
    textContent = contentNotNull
  }
}
