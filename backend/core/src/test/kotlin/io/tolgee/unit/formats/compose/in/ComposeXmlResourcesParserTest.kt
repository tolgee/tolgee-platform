package io.tolgee.unit.formats.compose.`in`

import io.tolgee.formats.compose.`in`.ComposeStringUnescaper
import io.tolgee.formats.xmlResources.StringUnit
import io.tolgee.formats.xmlResources.XmlResourcesStringValue
import io.tolgee.formats.xmlResources.XmlResourcesStringsModel
import io.tolgee.formats.xmlResources.`in`.XmlResourcesParser
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class ComposeXmlResourcesParserTest {
  @Test
  fun `it removes unsupported tags`() {
    "<unsupported><b>text</b></unsupported>".assertParsedTo("text", false)
  }

  @Test
  fun `replaces CDATA elements with inner text`() {
    "\n<![CDATA[<b>text</b>]]>\n".assertParsedTo("\n<b>text</b>\n", false)
  }

  @Test
  fun `sets wrapped with CDATA when the only node is wrapped with CDATA`() {
    "<![CDATA[<b>text</b>]]>".assertParsedTo("<b>text</b>", true)
  }

  @Test
  fun `it parses self-closing tag`() {
    "text <br/>".assertParsedTo("text ", false)
  }

  @Test
  fun `CDATA block is unescaped`() {
    """<![CDATA[<a href="Cool">text\n</a>]]>""".assertParsedTo("<a href=\"Cool\">text\n</a>", true)
  }

  @Test
  fun `correctly handles spaces between tags`() {
    "   <b> text </b><br/>  <b> a a a </b> <b></b> "
      .assertParsedTo("    text    a a a   ", false)
  }

  @Test
  fun `doesn't escape XML to entities for pure text`() {
    "I am just a text! &amp; &lt; &gt; &apos;"
      .assertParsedTo("I am just a text! & < > '", false)
  }

  @Test
  fun `parses element with attributes`() {
    "<a href=\"hey\" />"
      .assertParsedTo("", false)
  }

  @Test
  fun `doesnt unescape amp XML entity when XML context is removed`() {
    "I am just a text! <b>&amp;</b>"
      .assertParsedTo("I am just a text! &amp;", false)
  }

  private fun getReader(data: String): XMLEventReader {
    val inputFactory: XMLInputFactory = XMLInputFactory.newInstance()
    return inputFactory.createXMLEventReader(data.byteInputStream())
  }

  private fun getReaderWithSingleStringUnit(data: String): XMLEventReader {
    return getReader(
      """
      <resources>
        <string name="name">$data</string>
      </resources>
      """.trimIndent(),
    )
  }

  private fun parse(reader: XMLEventReader): XmlResourcesStringsModel {
    val parser =
      XmlResourcesParser(
        reader,
        ComposeStringUnescaper.defaultFactory,
        emptySet(),
      )
    return parser.parse()
  }

  private fun parseSingleStringUnit(data: String): XmlResourcesStringValue? {
    val unit = parse(getReaderWithSingleStringUnit(data)).items["name"] as StringUnit
    unit.value.assert.isNotNull()
    return unit.value
  }

  private fun String.assertParsedTo(
    expected: String,
    isWrappedWithCdata: Boolean,
  ) {
    val parsed = parseSingleStringUnit(this)
    parsed?.string.assert.isEqualTo(expected)
    parsed?.isWrappedCdata.assert.isEqualTo(isWrappedWithCdata)
  }
}
