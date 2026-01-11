package io.tolgee.unit.formats.android.out

import io.tolgee.formats.ExportFormat
import io.tolgee.formats.xmlResources.XmlResourcesStringValue
import io.tolgee.formats.xmlResources.out.TextToXmlResourcesConvertor
import io.tolgee.testing.assert
import org.assertj.core.api.AbstractStringAssert
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class TextToAndroidXmlConvertorTest {
  @Test
  fun `xml and placeholders is converted to CDATA`() {
    "<b>%s</b>".assertSingleCdataNodeText().isEqualTo("<b>%s</b>")
  }

  @Test
  fun `apostrophe is escaped in the HTML CDATA node`() {
    "<b>%s ' </b>".assertSingleCdataNodeText().isEqualTo("<b>%s \\' </b>")
  }

  @Test
  fun `double quotes and escape chars are escaped in the HTML CDATA node`() {
    "<b>%s \" \\ </b>".assertSingleCdataNodeText().isEqualTo("<b>%s \\\" \\\\ </b>")
  }

  @Test
  fun `more whitespaces are not converted in the HTML CDATA node`() {
    "<b>%s   </b>".assertSingleCdataNodeText().isEqualTo("<b>%s   </b>")
  }

  @Test
  fun `escaped newline is not double escaped text node`() {
    "\\n \\n".assertSingleTextNode().isEqualTo("\\n \\n")
  }

  @Test
  fun `it's possible to escape escape char before newline`() {
    "\\\\n \\\\n".assertSingleTextNode().isEqualTo("\\\\n \\\\n")
  }

  @Test
  fun `trailing spaces are handled`() {
    "%s     ".assertSingleTextNode().isEqualTo("%s\"     \"")
  }

  @Test
  fun `trailing percents are handled`() {
    "%s %%".assertSingleTextNode().isEqualTo("%s %%")
  }

  @Test
  fun `unsupported tags are converted to CDATA nodes`() {
    var nodes =
      "What a <unsupported attr=\"https://example.com\">link ' %% \" </unsupported>."
        .convertedNodes()
        .toList()
    nodes[0].assertTextContent("What a ")
    nodes[1].nodeAssertCdataNodeText(
      "<unsupported attr=\\\"https://example.com\\\">link \\' \\% \\\" " +
        "</unsupported>",
    )
    nodes[2].assertTextContent(".")

    nodes =
      (
        "What a <unsupported attr=\"https://example.com\">link ' %% %s \"    " +
          "</unsupported>."
      ).convertedNodes().toList()
    nodes[0].assertTextContent("What a ")
    nodes[1].nodeAssertCdataNodeText(
      "<unsupported attr=\\\"https://example.com\\\">link \\' %% %s \\\"    " +
        "</unsupported>",
    )
    nodes[2].assertTextContent(".")
  }

  @Test
  fun `all possible spaces are quoted`() {
    "a\n\t   \u0020 \u2008 \u2003a".assertSingleTextNode("a\\n\"\t   \u0020 \u2008 \u2003\"a")
  }

  @Test
  fun `it doesn't re-escape UTF symbols`() {
    "\\u0020\\u2008\\u2003".assertSingleTextNode("\\u0020\\u2008\\u2003")
  }

  @Test
  fun `converts capital U to lower`() {
    "\\U0020".assertSingleTextNode("\\u0020")
  }

  @Test
  fun `percent signs are escaped`() {
    "I am just a %% sign".assertSingleTextNode("I am just a \\% sign")
  }

  @Test
  fun `escapes in text nodes`() {
    val nodes = "'\"  <b></b>\n\n   \u0020\u2008\u2003".convertedNodes().toList()
    nodes[0].textContent.assert.isEqualTo("\\'\\\"\"  \"")
    nodes[1].nodeName.assert.isEqualTo("b")
    nodes[2].textContent.assert.isEqualTo("\"\n\n   \u0020\u2008\u2003\"")
  }

  @Test
  fun `new lines are escaped in cdata string`() {
    val nodes = "\n\n".convertedNodes(isWrappedWithCdata = true)
    nodes.getSingleNode().assertSingleCdataNodeText().isEqualTo("\\n\\n")
  }

  @Test
  fun `wrapping with CDATA works for invalid XML`() {
    val nodes = "<b> a ".convertedNodes(isWrappedWithCdata = true)
    nodes.getSingleNode().assertSingleCdataNodeText().isEqualTo("<b> a ")
  }

  @Test
  fun `multiple newlines are not quoted`() {
    "a\n\na".assertSingleTextNode("a\\n\\na")
  }

  @Test
  fun `doesnt fail with malformed XML`() {
    "<unclosed>tag".assertSingleTextNode().isEqualTo("<unclosed>tag")
  }

  @Test
  fun `escapes special characters in malformed XML`() {
    "<tag attr='value\">text with ' and \" characters</tag"
      .assertSingleTextNode()
      .isEqualTo("<tag attr=\\'value\\\">text with \\' and \\\" characters</tag")
  }

  @Test
  fun `escapes special characters in malformed XML #2`() {
    "text ' text <>"
      .assertSingleTextNode()
      .isEqualTo("text \\' text <>")
  }

  private fun Node.assertTextContent(text: String) {
    this.nodeType.assert.isEqualTo(Node.TEXT_NODE)
    this.textContent.assert.isEqualTo(text)
  }

  private fun String.assertSingleTextNode(text: String) {
    this.assertSingleTextNode().isEqualTo(text)
  }

  private fun String.assertSingleTextNode(): AbstractStringAssert<*> {
    val nodes = this.convertedNodes()
    return nodes.getSingleNode().textContent.assert
  }

  private fun Collection<Node>.getSingleNode(): Node {
    this.assert.hasSize(1)
    return this.single()
  }

  private fun Node.nodeAssertCdataNodeText(text: String) {
    this.nodeType.assert.isEqualTo(Node.CDATA_SECTION_NODE)
    this.textContent.assert.isEqualTo(text)
  }

  private fun String.assertSingleCdataNodeText(): AbstractStringAssert<*> {
    val node = this.convertedNodes().single()
    return node.assertSingleCdataNodeText()
  }

  private fun Node.assertSingleCdataNodeText(): AbstractStringAssert<*> {
    this.nodeType.assert.isEqualTo(Node.CDATA_SECTION_NODE)
    return this.textContent.assert
  }

  private fun String.getConverted(isWrappedWithCdata: Boolean = false) =
    TextToXmlResourcesConvertor(
      document,
      XmlResourcesStringValue(this, isWrappedWithCdata),
      ExportFormat.ANDROID_XML,
    ).convert()

  private fun String.convertedNodes(isWrappedWithCdata: Boolean = false): Collection<Node> {
    val result = this.getConverted(isWrappedWithCdata)
    result.text.assert.isNull()
    result.children.assert.isNotNull
    return result.children!!
  }

  private val document: Document by lazy {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    builder.newDocument()
  }
}
