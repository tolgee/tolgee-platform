package io.tolgee.ee.service.translationMemory.tmx

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TmxExporterTest {
  @Test
  fun `strips C0 control characters so a bad codepoint can't sink the export`() {
    // Production Sentry TOLGEE-BACKEND-2VE: a 0x0B (vertical tab) — commonly pasted from Word/Excel —
    // killed every TMX export for the affected TM because Woodstox refuses anything outside the
    // XML 1.0 Char range. The exporter must silently drop the illegal codepoint instead. Cover the
    // whole illegal C0 range (everything except tab/LF/CR) in one go to lock the contract.
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
    // Lone surrogates are not legal codepoints in any XML version and a stray one — easy to
    // produce via a JSON paste with broken UTF-16 — would otherwise crash export. The sanitiser
    // must walk codepoints so a valid surrogate pair (😀 = U+1F600) survives intact.
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
    // The two BMP noncharacters sit outside the XML 1.0 Char production and would otherwise be
    // rejected by Woodstox. They're rare in real content but trivial to leak through a broken
    // import — the sanitiser drops them. Built via Char(int) to dodge any source-encoding
    // round-trip mishaps on these edge codepoints.
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
  fun `preserves legal whitespace and astral characters during sanitisation`() {
    // Tab/LF/CR are legal in XML 1.0 and must survive sanitisation, otherwise segment text would
    // get its formatting silently mangled. Supplementary-plane codepoints (emoji etc.) also need
    // to pass through unchanged.
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
