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
}
