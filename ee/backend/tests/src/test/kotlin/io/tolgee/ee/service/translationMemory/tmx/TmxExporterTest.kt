package io.tolgee.ee.service.translationMemory.tmx

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TmxExporterTest {
  @Test
  fun `strips C0 control characters from segment text`() {
    val invalidControls = (0x00..0x1F).filter { it != 0x09 && it != 0x0A && it != 0x0D }
    val poisoned = invalidControls.joinToString(separator = "") { "x${it.toChar()}" }
    val unit =
      TmxExportUnit(
        tuid = "1",
        sourceText = poisoned,
        translations = listOf("de" to poisoned),
      )

    val xml = TmxExporter(sourceLanguageTag = "en", units = listOf(unit)).export().toString(Charsets.UTF_8)

    val clean = "x".repeat(invalidControls.size)
    assertThat(xml).contains("<seg>$clean</seg>")
    assertThat(xml.toCharArray().none { it.code in invalidControls }).isTrue()
  }

  @Test
  fun `strips lone UTF-16 surrogates while preserving valid supplementary codepoints`() {
    val unit =
      TmxExportUnit(
        tuid = "1",
        sourceText = "a\uD800b😀c\uDFFFd",
        translations = emptyList(),
      )

    val xml = TmxExporter(sourceLanguageTag = "en", units = listOf(unit)).export().toString(Charsets.UTF_8)

    assertThat(xml).contains("<seg>ab😀cd</seg>")
  }

  @Test
  fun `strips BMP noncharacters U+FFFE and U+FFFF`() {
    val unit =
      TmxExportUnit(
        tuid = "1",
        sourceText = "a${Char(0xFFFE)}b${Char(0xFFFF)}c",
        translations = emptyList(),
      )

    val xml = TmxExporter(sourceLanguageTag = "en", units = listOf(unit)).export().toString(Charsets.UTF_8)

    assertThat(xml).contains("<seg>abc</seg>")
  }

  @Test
  fun `preserves legal whitespace and astral characters`() {
    val unit =
      TmxExportUnit(
        tuid = "1",
        sourceText = "line1\nline2\tcol\rend 😀",
        translations = emptyList(),
      )

    val xml = TmxExporter(sourceLanguageTag = "en", units = listOf(unit)).export().toString(Charsets.UTF_8)

    assertThat(xml).contains("line1\nline2\tcol")
    assertThat(xml).contains("😀")
  }
}
