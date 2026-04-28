package io.tolgee.ee.api.v2.controllers.translationMemory

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile

@SpringBootTest
@AutoConfigureMockMvc
class TranslationMemoryTmxControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var translationMemoryEntryRepository: TranslationMemoryEntryRepository

  lateinit var testData: TranslationMemoryTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private val orgId get() = testData.projectWithoutTm.organizationOwner.id

  // sharedTm has sourceLanguageTag="en" and 2 entries:
  //   "Hello world" -> "Hallo Welt" (de)
  //   "Thank you" -> "Danke" (de)
  private val sharedTmId get() = testData.sharedTm.id

  // unassignedSharedTm has sourceLanguageTag="en" and 0 entries
  private val emptyTmId get() = testData.unassignedSharedTm.id

  private fun exportUrl(tmId: Long) = "/v2/organizations/$orgId/translation-memories/$tmId/export"

  private fun importUrl(tmId: Long) = "/v2/organizations/$orgId/translation-memories/$tmId/import"

  // ---------- Export ----------

  @Test
  fun `exports TMX with correct structure`() {
    val result = performAuthGet(exportUrl(sharedTmId)).andIsOk.andReturn()
    val xml = result.response.contentAsString

    assertThat(xml).contains("<tmx")
    assertThat(xml).contains("version=\"1.4\"")
    assertThat(xml).contains("srclang=\"en\"")
    assertThat(xml).contains("<tu")
    assertThat(xml).contains("<tuv")
    assertThat(xml).contains("<seg>")
    // Should contain source texts
    assertThat(xml).contains("Hello world")
    assertThat(xml).contains("Thank you")
    // Should contain target texts (de + fr)
    assertThat(xml).contains("Hallo Welt")
    assertThat(xml).contains("Bonjour le monde")
    assertThat(xml).contains("Danke")
  }

  @Test
  fun `export has correct content-type and filename`() {
    val result = performAuthGet(exportUrl(sharedTmId)).andIsOk.andReturn()
    assertThat(result.response.contentType).contains("application/xml")
    assertThat(result.response.getHeader("Content-Disposition")).contains("attachment")
    assertThat(result.response.getHeader("Content-Disposition")).contains(".tmx")
  }

  @Test
  fun `exports empty TMX for TM with no entries`() {
    val result = performAuthGet(exportUrl(emptyTmId)).andIsOk.andReturn()
    val xml = result.response.contentAsString

    assertThat(xml).contains("<tmx")
    assertThat(xml).contains("<body")
    // No <tu> elements
    assertThat(xml).doesNotContain("<tu")
  }

  // ---------- Import ----------

  @Test
  fun `import creates entries with correct tuid and language tags`() {
    val tmx =
      buildTmx(
        "en",
        listOf(
          tu("New source", mapOf("de" to "Neue Quelle", "fr" to "Nouvelle source")),
        ),
      )
    val file = mockTmxFile(tmx)

    performAuthMultipart(importUrl(emptyTmId), listOf(file))
      .andIsOk
      .andAssertThatJson {
        node("created").isEqualTo(1)
        node("updated").isEqualTo(0)
        node("skipped").isEqualTo(0)
      }

    val entries = translationMemoryEntryRepository.findByTranslationMemoryId(emptyTmId)
    assertThat(entries).hasSize(2)
    assertThat(entries.map { it.sourceText }).allMatch { it == "New source" }
    assertThat(entries.map { it.targetLanguageTag }).containsExactlyInAnyOrder("de", "fr")
    assertThat(entries.map { it.tuid }).allMatch { it == "1" }
  }

  @Test
  fun `import keep mode preserves existing entries matched by tuid`() {
    // First import to set up entries with tuids
    val initial =
      buildTmx(
        "en",
        listOf(
          tu("Hello", mapOf("de" to "Hallo")),
          tu("Bye", mapOf("de" to "Tschüss")),
        ),
      )
    performAuthMultipart(importUrl(emptyTmId), listOf(mockTmxFile(initial))).andIsOk

    // Re-import with same tuids but different translations — keep mode should skip
    val reimport =
      buildTmx(
        "en",
        listOf(
          tu("Hello", mapOf("de" to "Hallo geändert")), // tuid=1, same lang → conflict → skip
          tu("Bye", mapOf("de" to "Auf Wiedersehen")), // tuid=2, same lang → conflict → skip
        ),
      )
    performAuthMultipart(
      importUrl(emptyTmId),
      listOf(mockTmxFile(reimport)),
      mapOf("overrideExisting" to arrayOf("false")),
    ).andIsOk
      .andAssertThatJson {
        node("created").isEqualTo(0)
        node("skipped").isEqualTo(2)
      }

    // Original translations unchanged
    val entries = translationMemoryEntryRepository.findByTranslationMemoryId(emptyTmId)
    assertThat(entries.first { it.tuid == "1" }.targetText).isEqualTo("Hallo")
  }

  @Test
  fun `import override mode updates entries matched by tuid`() {
    // First import
    val initial =
      buildTmx(
        "en",
        listOf(
          tu("Hello", mapOf("de" to "Hallo")),
        ),
      )
    performAuthMultipart(importUrl(emptyTmId), listOf(mockTmxFile(initial))).andIsOk

    // Re-import with same tuid but different translation — override
    val reimport =
      buildTmx(
        "en",
        listOf(
          tu("Hello", mapOf("de" to "Hallo Welt")), // tuid=1 → conflict → override
        ),
      )
    performAuthMultipart(
      importUrl(emptyTmId),
      listOf(mockTmxFile(reimport)),
      mapOf("overrideExisting" to arrayOf("true")),
    ).andIsOk
      .andAssertThatJson {
        node("created").isEqualTo(0)
        node("updated").isEqualTo(1)
        node("skipped").isEqualTo(0)
      }

    val entries = translationMemoryEntryRepository.findByTranslationMemoryId(emptyTmId)
    assertThat(entries.first { it.tuid == "1" }.targetText).isEqualTo("Hallo Welt")
  }

  @Test
  fun `import override mode skips entries when content is identical`() {
    val initial =
      buildTmx(
        "en",
        listOf(
          tu("Hello", mapOf("de" to "Hallo")),
        ),
      )
    performAuthMultipart(importUrl(emptyTmId), listOf(mockTmxFile(initial))).andIsOk

    // Same content, same tuid → skip even in override mode
    performAuthMultipart(
      importUrl(emptyTmId),
      listOf(mockTmxFile(initial)),
      mapOf("overrideExisting" to arrayOf("true")),
    ).andIsOk
      .andAssertThatJson {
        node("updated").isEqualTo(0)
        node("skipped").isEqualTo(1)
      }
  }

  @Test
  fun `import always creates entries that have no tuid`() {
    // Import same source text twice with no tuid — both should be created
    // "created" counts distinct tuids; null tuid is counted as a single unit
    val tmx =
      buildTmxRaw(
        "en",
        listOf(
          TuRaw(null, "Same source", mapOf("de" to "Erste")),
          TuRaw(null, "Same source", mapOf("de" to "Zweite")),
        ),
      )
    performAuthMultipart(importUrl(emptyTmId), listOf(mockTmxFile(tmx)))
      .andIsOk
      .andAssertThatJson {
        node("created").isEqualTo(1) // null tuid counted as one unit
      }

    val entries = translationMemoryEntryRepository.findByTranslationMemoryId(emptyTmId)
    assertThat(entries).hasSize(2)
    assertThat(entries.map { it.tuid }).allMatch { it == null }
  }

  @Test
  fun `import creates separate entry when tuid differs even for same source text`() {
    // First import with tuid=1
    val initial =
      buildTmx(
        "en",
        listOf(
          tu("Hello", mapOf("de" to "Hallo")),
        ),
      )
    performAuthMultipart(importUrl(emptyTmId), listOf(mockTmxFile(initial))).andIsOk

    // Import same source text but different tuid — should create, not conflict
    val second =
      buildTmxRaw(
        "en",
        listOf(
          TuRaw("different-id", "Hello", mapOf("de" to "Hallo anders")),
        ),
      )
    performAuthMultipart(importUrl(emptyTmId), listOf(mockTmxFile(second)))
      .andIsOk
      .andAssertThatJson {
        node("created").isEqualTo(1)
      }

    val entries = translationMemoryEntryRepository.findByTranslationMemoryId(emptyTmId)
    assertThat(entries).hasSize(2)
  }

  @Test
  fun `import returns 400 for malformed XML`() {
    val file = MockMultipartFile("file", "bad.tmx", "application/xml", "not xml at all".toByteArray())
    performAuthMultipart(importUrl(emptyTmId), listOf(file)).andIsBadRequest
  }

  @Test
  fun `export then import round-trip preserves entry data and tuids`() {
    // Import initial data with tuids
    val initial =
      buildTmx(
        "en",
        listOf(
          tu("Hello", mapOf("de" to "Hallo")),
          tu("Bye", mapOf("de" to "Tschüss")),
        ),
      )
    performAuthMultipart(importUrl(emptyTmId), listOf(mockTmxFile(initial))).andIsOk

    // Export
    val exported =
      performAuthGet(exportUrl(emptyTmId))
        .andIsOk
        .andReturn()
        .response.contentAsString
    assertThat(exported).contains("tuid=")

    // Import into sharedTm (different TM)
    performAuthMultipart(
      importUrl(sharedTmId),
      listOf(MockMultipartFile("file", "rt.tmx", "application/xml", exported.toByteArray())),
    ).andIsOk
      .andAssertThatJson {
        node("created").isEqualTo(2)
      }
  }

  // ---------- Helpers ----------

  private data class TuData(
    val source: String,
    val targets: Map<String, String>,
  )

  private data class TuRaw(
    val tuid: String?,
    val source: String,
    val targets: Map<String, String>,
  )

  private fun tu(
    source: String,
    targets: Map<String, String>,
  ) = TuData(source, targets)

  // Helper: builds TMX XML with auto-assigned sequential tuids starting at 1
  private fun buildTmx(
    srcLang: String,
    units: List<TuData>,
  ): String {
    return buildTmxRaw(srcLang, units.mapIndexed { i, u -> TuRaw((i + 1).toString(), u.source, u.targets) })
  }

  // Helper: builds TMX XML with explicit (or null) tuids for each <tu>
  private fun buildTmxRaw(
    srcLang: String,
    units: List<TuRaw>,
  ): String {
    val sb = StringBuilder()
    sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    sb.appendLine("""<tmx version="1.4">""")
    sb.appendLine("""  <header srclang="$srcLang" datatype="PlainText" creationtool="test"/>""")
    sb.appendLine("""  <body>""")
    units.forEach { unit ->
      val tuidAttr = if (unit.tuid != null) """ tuid="${unit.tuid}"""" else ""
      sb.appendLine("""    <tu$tuidAttr>""")
      sb.appendLine("""      <tuv xml:lang="$srcLang"><seg>${unit.source}</seg></tuv>""")
      unit.targets.forEach { (lang, text) ->
        sb.appendLine("""      <tuv xml:lang="$lang"><seg>$text</seg></tuv>""")
      }
      sb.appendLine("""    </tu>""")
    }
    sb.appendLine("""  </body>""")
    sb.appendLine("""</tmx>""")
    return sb.toString()
  }

  private fun mockTmxFile(content: String): MockMultipartFile {
    return MockMultipartFile("file", "test.tmx", "application/xml", content.toByteArray())
  }
}
