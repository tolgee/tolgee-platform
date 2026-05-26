package io.tolgee.unit.formats.po.`in`

import io.tolgee.formats.po.`in`.PoParser
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.util.FileProcessorContextMockUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PoParserTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `returns correct parsed result`() {
    mockUtil.mockIt("example.po", "src/test/resources/import/po/example.po")
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
  fun `parses msgctxt for each entry`() {
    mockUtil.mockIt("example_msgctxt.po", "src/test/resources/import/po/example_msgctxt.po")
    val result = PoParser(mockUtil.fileProcessorContext)()
    val byMsgid =
      result.translations
        .filter { it.msgid.toString().isNotEmpty() }
        .associateBy { it.msgctxt.toString() to it.msgid.toString() }

    assertThat(byMsgid).containsKey("menu" to "Open")
    assertThat(byMsgid).containsKey("verb" to "Open")
    assertThat(byMsgid).containsKey("" to "Open")
    assertThat(byMsgid["items" to "%d item"]?.msgidPlural?.toString()).isEqualTo("%d items")
  }

  @Test
  fun `parses msgctxt containing escaped quotes and newlines`() {
    mockUtil.mockIt("example_msgctxt_escapes.po", "src/test/resources/import/po/example_msgctxt_escapes.po")
    val result = PoParser(mockUtil.fileProcessorContext)()
    val entry = result.translations.first { it.msgid.toString() == "Save" }
    assertThat(entry.msgctxt.toString()).isEqualTo("a \"quoted\" ctx\nwith newline")
  }

  @Test
  fun `does not emit PO_MSGCTXT_NOT_SUPPORTED issue`() {
    mockUtil.mockIt("example_msgctxt.po", "src/test/resources/import/po/example_msgctxt.po")
    PoParser(mockUtil.fileProcessorContext)()
    val emittedTypes =
      mockUtil.fileProcessorContext.fileEntity.issues
        .map { it.type }
    assertThat(emittedTypes).doesNotContain(FileIssueType.PO_MSGCTXT_NOT_SUPPORTED)
  }

  @Test
  fun `does not promote msgctxt-only entry with empty msgid to header`() {
    mockUtil.mockIt("example_msgctxt_only.po", "src/test/resources/import/po/example_msgctxt_only.po")
    val result = PoParser(mockUtil.fileProcessorContext)()
    // The msgctxt-only-with-empty-msgid entry must not contribute header metadata
    // (would otherwise be parsed as Key: value lines from its msgstr).
    assertThat(result.meta.language).isEqualTo("de")
    assertThat(result.meta.other).doesNotContainKey("garbage")
  }
}
