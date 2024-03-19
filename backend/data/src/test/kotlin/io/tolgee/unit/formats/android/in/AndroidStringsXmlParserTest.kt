package io.tolgee.unit.formats.android.`in`

import AndroidStringsXmlParser
import io.tolgee.formats.android.AndroidStringValue
import io.tolgee.formats.android.AndroidStringsXmlModel
import io.tolgee.formats.android.StringUnit
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
    "<a><b>text</b></a>".assertParsedTo("text", false)
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
  fun `correctly handles spaces between tags`() {
    "   <b> text </b><br/>  <b> a a a </b> <b></b> "
      .assertParsedTo("<b>text</b><br/> <b>a a a</b> <b></b>", false)
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

  private fun parse(reader: XMLEventReader): AndroidStringsXmlModel {
    val parser = AndroidStringsXmlParser(reader)
    return parser.parse()
  }

  private fun parseSingleStringUnit(data: String): AndroidStringValue? {
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
