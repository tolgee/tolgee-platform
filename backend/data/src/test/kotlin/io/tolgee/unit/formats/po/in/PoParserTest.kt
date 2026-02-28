package io.tolgee.unit.formats.po.`in`

import io.tolgee.formats.po.`in`.PoParser
import io.tolgee.util.FileProcessorContextMockUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PoParserTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.po", "src/test/resources/import/po/example.po")
  }

  @Test
  fun `returns correct parsed result`() {
    val result = PoParser(mockUtil.fileProcessorContext)()
    assertThat(result.translations).hasSizeGreaterThan(8)
    assertThat(result.translations[5].msgstrPlurals).hasSize(2)
    assertThat(result.translations[5].msgstrPlurals!![0].toString()).isEqualTo("Eine Seite gelesen wurde.")
    assertThat(result.translations[5].msgstrPlurals!![1].toString()).isEqualTo("%d Seiten gelesen wurden.")
    assertThat(result.translations[2].meta.translatorComments).hasSize(2)
    assertThat(result.translations[2].meta.translatorComments[0]).isEqualTo("some other comment")
    assertThat(result.translations[2].meta.extractedComments).hasSize(4)
    assertThat(result.translations[9].msgstr.toString()).isEqualTo("Dies ist ein \"zitat\"")
    assertThat(result.translations[9].msgid.toString()).isEqualTo("This is a \"quote\"")
    assertThat(result.translations[10].msgstr.toString()).isEqualTo("This\nis\na\nmultiline\nstring")
    assertThat(result.translations[11].msgstr.toString()).isEqualTo("This\r\nis\r\na\r\nmultiline\r\nstring")
  }

  @Test
  fun `parses msgctxt correctly`() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.po", "src/test/resources/import/po/example_msgctxt.po")
    val result = PoParser(mockUtil.fileProcessorContext)()
    // header + 5 entries (menu/Open, menu/Close, Hello, dialog/Open, stats/%d file plural)
    assertThat(result.translations).hasSize(6)

    // msgctxt "menu", msgid "Open"
    assertThat(result.translations[1].msgctxt.toString()).isEqualTo("menu")
    assertThat(result.translations[1].msgid.toString()).isEqualTo("Open")
    assertThat(result.translations[1].msgstr.toString()).isEqualTo("Öffnen")

    // msgctxt "menu", msgid "Close"
    assertThat(result.translations[2].msgctxt.toString()).isEqualTo("menu")
    assertThat(result.translations[2].msgid.toString()).isEqualTo("Close")

    // no msgctxt, msgid "Hello"
    assertThat(result.translations[3].msgctxt.toString()).isEqualTo("")
    assertThat(result.translations[3].msgid.toString()).isEqualTo("Hello")

    // msgctxt "dialog", msgid "Open"
    assertThat(result.translations[4].msgctxt.toString()).isEqualTo("dialog")
    assertThat(result.translations[4].msgid.toString()).isEqualTo("Open")

    // msgctxt "stats", plural entry
    assertThat(result.translations[5].msgctxt.toString()).isEqualTo("stats")
    assertThat(result.translations[5].msgid.toString()).isEqualTo("%d file")
    assertThat(result.translations[5].msgidPlural.toString()).isEqualTo("%d files")
    assertThat(result.translations[5].msgstrPlurals).hasSize(2)
  }
}
