package io.tolgee.ee.service.translationMemory.tmx

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TmxParserTest {
  @Test
  fun `uses TM source language as authoritative, ignoring TMX srclang`() {
    // TMX declares English as source but the TM we're importing into has cs as its base.
    // Without this normalisation the entries would land with source_text in English,
    // never matching cs-base projects via the source_text = baseTranslation lookup.
    val tmx = buildTmx(srclang = "en")
    val entries = TmxParser(tmSourceLanguageTag = "cs").parse(tmx.byteInputStream())

    assertThat(entries).hasSize(2)
    assertThat(entries.map { it.sourceText }).allMatch { it == "Uložit změny" }
    assertThat(entries.map { it.targetLanguageTag }).containsExactlyInAnyOrder("en", "sk")
    assertThat(entries.first { it.targetLanguageTag == "en" }.targetText).isEqualTo("Save changes")
    assertThat(entries.first { it.targetLanguageTag == "sk" }.targetText).isEqualTo("Uložiť zmeny")
  }

  @Test
  fun `skips tus that don't carry a tuv in the TM's source language`() {
    // Same TMX as above but the TM expects fr as source — there's no fr tuv in the TU.
    val tmx = buildTmx(srclang = "en")
    val entries = TmxParser(tmSourceLanguageTag = "fr").parse(tmx.byteInputStream())

    assertThat(entries).isEmpty()
  }

  @Test
  fun `matches the TM source language case-insensitively`() {
    // TMX `<tuv xml:lang>` uses the BCP-47 convention of capitalising region (e.g. en-US, sr-Latn).
    // The TM stores plain "en" but the TMX file's tuv could be "EN" or mixed-case.
    val tmx = """<?xml version="1.0" encoding="UTF-8"?>
      <tmx version="1.4">
        <header srclang="*" />
        <body>
          <tu tuid="1">
            <tuv xml:lang="EN"><seg>Hello</seg></tuv>
            <tuv xml:lang="cs"><seg>Ahoj</seg></tuv>
          </tu>
        </body>
      </tmx>"""
    val entries = TmxParser(tmSourceLanguageTag = "en").parse(tmx.byteInputStream())

    assertThat(entries).hasSize(1)
    assertThat(entries.single().sourceText).isEqualTo("Hello")
    assertThat(entries.single().targetText).isEqualTo("Ahoj")
  }

  private fun buildTmx(srclang: String): String =
    """<?xml version="1.0" encoding="UTF-8"?>
      <tmx version="1.4">
        <header srclang="$srclang" datatype="PlainText" creationtool="test" />
        <body>
          <tu tuid="1">
            <tuv xml:lang="en"><seg>Save changes</seg></tuv>
            <tuv xml:lang="cs"><seg>Uložit změny</seg></tuv>
            <tuv xml:lang="sk"><seg>Uložiť zmeny</seg></tuv>
          </tu>
        </body>
      </tmx>"""
}
