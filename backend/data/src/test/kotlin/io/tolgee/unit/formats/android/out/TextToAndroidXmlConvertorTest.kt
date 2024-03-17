package io.tolgee.unit.formats.android.out

import io.tolgee.formats.android.out.TextToAndroidXmlConvertor
import io.tolgee.testing.assert
import org.assertj.core.api.AbstractStringAssert
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class TextToAndroidXmlConvertorTest {
  @Test
  fun `null returns null`() {
    null.getConverted().assert.isNull()
  }

  @Test
  fun `xml and placeholders is converted to CDATA`() {
    "<b>%s</b>".assertSingleCdataNodeText().isEqualTo("<b>%s</b>")
  }

  @Test
  fun `apostrophe is escaped in the HTML CDATA node`() {
    "<b>%s ' </b>".assertSingleCdataNodeText().isEqualTo("<b>%s \\' </b>")
  }

  @Test
  fun `double quotes are escaped in the HTML CDATA node`() {
    "<b>%s \" </b>".assertSingleCdataNodeText().isEqualTo("<b>%s \\\" </b>")
  }

  @Test
  fun `more whitespaces are not converted in the HTML CDATA node`() {
    "<b>%s   </b>".assertSingleCdataNodeText().isEqualTo("<b>%s   </b>")
  }

  @Test
  fun `unsupported tags are converted to CDATA nodes`() {
    var nodes = "What a <a href=\"https://example.com\">link ' %% \" </a>.".convertedNodes().toList()
    nodes[0].assertTextContent("What a ")
    nodes[1].nodeAssertCdataNodeText("<a href=\\\"https://example.com\\\">link \\' % \\\" </a>")
    nodes[2].assertTextContent(".")

    nodes = "What a <a href=\"https://example.com\">link ' %% %s \"    </a>.".convertedNodes().toList()
    nodes[0].assertTextContent("What a ")
    nodes[1].nodeAssertCdataNodeText("<a href=\\\"https://example.com\\\">link \\' %% %s \\\"    </a>")
    nodes[2].assertTextContent(".")
  }

  @Test
  fun `escapes in text nodes`() {
    val nodes = "'\"  <b></b>\n\n   \u0020\u2008\u2003".convertedNodes().toList()
    nodes[0].textContent.assert.isEqualTo("\'\\\"\"  \"")
    nodes[1].nodeName.assert.isEqualTo("b")
    nodes[2].textContent.assert.isEqualTo("\\n\\n\"   \u0020\u2008\u2003\"")
  }

  private fun Node.assertTextContent(text: String) {
    this.nodeType.assert.isEqualTo(Node.TEXT_NODE)
    this.textContent.assert.isEqualTo(text)
  }

  private fun Node.nodeAssertCdataNodeText(text: String) {
    this.nodeType.assert.isEqualTo(Node.CDATA_SECTION_NODE)
    this.textContent.assert.isEqualTo(text)
  }

  private fun String.assertSingleCdataNodeText(): AbstractStringAssert<*> {
    val node = this.convertedNodes().single()
    node.nodeType.assert.isEqualTo(Node.CDATA_SECTION_NODE)
    return node.textContent.assert
  }

  private fun String?.getConverted() = TextToAndroidXmlConvertor().getContent(document, this)

  private fun String?.convertedNodes(): Collection<Node> {
    val result = getConverted()
    result?.text.assert.isNull()
    result?.children.assert.isNotNull
    return result!!.children!!
  }

  private val document: Document by lazy {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    builder.newDocument()
  }
}
