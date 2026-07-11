package io.tolgee.ee.service.translationMemory.tmx

import io.tolgee.model.translationMemory.TranslationMemoryEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TmxParserTest {
  @Test
  fun `uses TM source language as authoritative, ignoring TMX srclang`() {
    // TMX declares English as source but the TM we're importing into has cs as its base.
    // Without this normalisation the entries would land with source_text in English,
    // never matching cs-base projects via the source_text = baseTranslation lookup.
    val tmx = buildTmx(srclang = "en")
    val result = TmxParser(tmSourceLanguageTag = "cs").parse(tmx.byteInputStream())

    assertThat(result.entries).hasSize(2)
    assertThat(result.entries.map { it.sourceText }).allMatch { it == "Uložit změny" }
    assertThat(result.entries.map { it.targetLanguageTag }).containsExactlyInAnyOrder("en", "sk")
    assertThat(result.entries.first { it.targetLanguageTag == "en" }.targetText).isEqualTo("Save changes")
    assertThat(result.entries.first { it.targetLanguageTag == "sk" }.targetText).isEqualTo("Uložiť zmeny")
    assertThat(result.skippedOversize).isZero
  }

  @Test
  fun `skips tus that don't carry a tuv in the TM's source language`() {
    // Same TMX as above but the TM expects fr as source — there's no fr tuv in the TU.
    val tmx = buildTmx(srclang = "en")
    val result = TmxParser(tmSourceLanguageTag = "fr").parse(tmx.byteInputStream())

    assertThat(result.entries).isEmpty()
    assertThat(result.skippedOversize).isZero
  }

  @Test
  fun `drops tus whose source segment exceeds the size cap and counts the loss`() {
    // Mirrors the DTO cap on manual entries — TMX import otherwise bypasses validation and
    // could land multi-MB rows. The drop must be reported via skippedOversize so the import
    // response can surface it (instead of long segments vanishing without trace).
    val oversize = "x".repeat(TranslationMemoryEntry.MAX_TEXT_LENGTH + 1)
    val tmx = """<?xml version="1.0" encoding="UTF-8"?>
      <tmx version="1.4">
        <header srclang="*" />
        <body>
          <tu tuid="oversize">
            <tuv xml:lang="en"><seg>$oversize</seg></tuv>
            <tuv xml:lang="cs"><seg>Ahoj</seg></tuv>
            <tuv xml:lang="de"><seg>Hallo</seg></tuv>
          </tu>
          <tu tuid="ok"><tuv xml:lang="en"><seg>Hi</seg></tuv><tuv xml:lang="cs"><seg>Ahoj</seg></tuv></tu>
        </body>
      </tmx>"""
    val result = TmxParser(tmSourceLanguageTag = "en").parse(tmx.byteInputStream())

    assertThat(result.entries).hasSize(1)
    assertThat(result.entries.single().sourceText).isEqualTo("Hi")
    // 2 non-source tuvs (cs, de) on the dropped tu — both would have produced an entry.
    assertThat(result.skippedOversize).isEqualTo(2)
  }

  @Test
  fun `drops target segments exceeding the size cap while keeping the rest of the tu`() {
    val oversize = "y".repeat(TranslationMemoryEntry.MAX_TEXT_LENGTH + 1)
    val tmx = """<?xml version="1.0" encoding="UTF-8"?>
      <tmx version="1.4">
        <header srclang="*" />
        <body>
          <tu tuid="1">
            <tuv xml:lang="en"><seg>Hello</seg></tuv>
            <tuv xml:lang="cs"><seg>Ahoj</seg></tuv>
            <tuv xml:lang="de"><seg>$oversize</seg></tuv>
          </tu>
        </body>
      </tmx>"""
    val result = TmxParser(tmSourceLanguageTag = "en").parse(tmx.byteInputStream())

    assertThat(result.entries).hasSize(1)
    assertThat(result.entries.single().targetLanguageTag).isEqualTo("cs")
    assertThat(result.entries.single().targetText).isEqualTo("Ahoj")
    assertThat(result.skippedOversize).isEqualTo(1)
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
    val result = TmxParser(tmSourceLanguageTag = "en").parse(tmx.byteInputStream())

    assertThat(result.entries).hasSize(1)
    assertThat(result.entries.single().sourceText).isEqualTo("Hello")
    assertThat(result.entries.single().targetText).isEqualTo("Ahoj")
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
