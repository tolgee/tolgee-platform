package io.tolgee.unit.formats.android.`in`

import io.tolgee.formats.android.`in`.AndroidStringUnescaper
import io.tolgee.formats.xmlResources.StringUnit
import io.tolgee.formats.xmlResources.XmlResourcesParsingConstants
import io.tolgee.formats.xmlResources.XmlResourcesStringValue
import io.tolgee.formats.xmlResources.XmlResourcesStringsModel
import io.tolgee.formats.xmlResources.`in`.XmlResourcesParser
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AndroidStringsXmlParserTest {
  @Test
  fun `quoted string is parsed correctly`() {
    "\"  \"".assertParsedTo("  ", false)
  }

  @Test
  fun `it unescapes correctly`() {
    "\\' \\\" \\n<b>\\' \\\" \\n</b>\\' \\\" \\n".assertParsedTo("' \" \n<b>' \" \n</b>' \" \n", false)
  }

  @Test
  fun `it removes unsupported tags`() {
    "<unsupported><b>text</b></unsupported>".assertParsedTo("text", false)
  }

  @Test
  fun `replaces CDATA elements with inner text`() {
    "\n<![CDATA[<b>text</b>]]>\n".assertParsedTo("<b>text</b>", true)
  }

  @Test
  fun `it parses self-closing tag`() {
    "text <br/>".assertParsedTo("text <br/>", false)
  }

  @Test
  fun `CDATA block is unescaped`() {
    """<![CDATA[<a href=\"Cool\">text</a>]]>""".assertParsedTo("<a href=\"Cool\">text</a>", true)
  }

  @Test
  fun `correctly handles spaces between tags`() {
    "   <b> text </b><br/>  <b> a a a </b> <b></b> "
      .assertParsedTo("<b>text</b><br/> <b>a a a</b> <b></b>", false)
  }

  @Test
  fun `doesn't escape XML to entities for pure text`() {
    "I am just a text! &amp; &lt; &gt; &apos;"
      .assertParsedTo("I am just a text! & < > '", false)
  }

  @Test
  fun `parses element with attributes`() {
    "<a href=\"hey\" />"
      .assertParsedTo("<a href=\"hey\" />", false)
  }

  @Test
  fun `doesnt unescape amp XML entity in XML context`() {
    "I am just a text! <b>&amp;</b>"
      .assertParsedTo("I am just a text! <b>&amp;</b>", false)
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
        AndroidStringUnescaper.defaultFactory,
        XmlResourcesParsingConstants.androidSupportedTags,
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
