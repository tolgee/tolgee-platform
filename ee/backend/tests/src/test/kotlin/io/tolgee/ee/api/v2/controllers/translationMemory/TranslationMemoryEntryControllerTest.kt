package io.tolgee.ee.api.v2.controllers.translationMemory

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.translationMemory.CreateMultipleTranslationMemoryEntriesRequest
import io.tolgee.ee.data.translationMemory.CreateMultipleTranslationMemoryEntriesTranslationRequest
import io.tolgee.ee.data.translationMemory.DeleteMultipleTranslationMemoryEntriesRequest
import io.tolgee.ee.data.translationMemory.TranslationMemoryEntryRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationMemoryEntryControllerTest : AuthorizedControllerTest() {
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
  private val sharedTmId get() = testData.sharedTm.id

  @Test
  fun `returns entries grouped by source text with correct group count`() {
    performAuthGet("/v2/organizations/$orgId/translation-memories/$sharedTmId/entries")
      .andIsOk
      .andAssertThatJson {
        // Shared TM: 2 distinct source texts ("Hello world" de+fr, "Thank you" de)
        node("_embedded.translationMemoryRows").isArray.hasSize(2)
      }
  }

  @Test
  fun `search matches both source and target text case-insensitively`() {
    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries?search=hallo",
    ).andIsOk
      .andAssertThatJson {
        // "hallo" matches the German target "Hallo Welt" — the group's source text is "Hello world"
        node("_embedded.translationMemoryRows").isArray.hasSize(1)
        node("_embedded.translationMemoryRows[0].sourceText").isEqualTo("Hello world")
      }
  }

  @Test
  fun `language filter includes entries matching the tag`() {
    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries?targetLanguageTag=de",
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemoryRows").isArray.hasSize(2)
        // Each row has 1 German cell
        node("_embedded.translationMemoryRows[0].cells").isArray.hasSize(1)
        node("_embedded.translationMemoryRows[1].cells").isArray.hasSize(1)
      }
  }

  @Test
  fun `language filter returns empty entries for unmatched languages without hiding source rows`() {
    // Pagination is by distinct source text, so the language filter only narrows which
    // entries are returned per group — it does NOT exclude source texts that have
    // no translation in the filtered language.
    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries?targetLanguageTag=fr",
    ).andIsOk
      .andAssertThatJson {
        // Both source texts still paginate (totalElements = 2)
        node("page.totalElements").isEqualTo(2)
        node("_embedded.translationMemoryRows").isArray.hasSize(2)
        // "Hello world" has 1 French entry; "Thank you" has none
      }
  }

  @Test
  fun `project TM virtual content paginates without repeating source texts across pages`() {
    // Regression: previous implementation loaded ALL virtual rows on every page request, so
    // scrolling repeatedly returned the same source texts and the content view kept loading
    // forever for projects with many translations. With proper SQL pagination, page 0 and
    // page 1 must contain disjoint source texts.
    val projectTmId = testData.projectTm.id
    val sourceTexts = mutableSetOf<String>()

    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$projectTmId/entries?size=1&page=0",
    ).andIsOk
      .andAssertThatJson {
        // projectWithTm has 4 keys with German targets — existing/reviewed/promoted/demoted.
        node("page.totalElements").isEqualTo(4)
        node("page.totalPages").isEqualTo(4)
        node("_embedded.translationMemoryRows").isArray.hasSize(1)
      }.andReturn()
      .response
      .contentAsString
      .let { sourceTexts += extractSourceTexts(it) }

    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$projectTmId/entries?size=1&page=1",
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemoryRows").isArray.hasSize(1)
      }.andReturn()
      .response
      .contentAsString
      .let { sourceTexts += extractSourceTexts(it) }

    // Disjoint pages → distinct source texts seen across the two requests, not the same repeated.
    assertThat(sourceTexts).hasSize(2)
  }

  private fun extractSourceTexts(json: String): List<String> {
    val regex = Regex("\"sourceText\"\\s*:\\s*\"([^\"]+)\"")
    return regex.findAll(json).map { it.groupValues[1] }.toList()
  }

  @Test
  fun `project TM listing merges manual stored entries with virtual rows from project translations`() {
    // PROJECT TMs return virtual content from the assigned project AND any manual entries the
    // user added on the TM itself. Manual entries are interleaved with virtual rows by source
    // text so the user sees one unified list.
    val projectTmId = testData.projectTm.id
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = "Manual phrase"
        targetText = "Frase manual"
        targetLanguageTag = "es"
      }
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$projectTmId/entries",
      request,
    ).andIsOk

    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$projectTmId/entries",
    ).andIsOk
      .andAssertThatJson {
        // 4 virtual rows from projectWithTm's German targets (Demotion / Existing / Promoted /
        // Reviewed) + 1 manual ("Manual phrase").
        node("page.totalElements").isEqualTo(5)
        node("_embedded.translationMemoryRows").isArray.hasSize(5)
        // Alphabetical order by source_text — manual ("Manual phrase") slots between virtual rows.
        node("_embedded.translationMemoryRows[0].sourceText").isEqualTo("Demotion source")
        node("_embedded.translationMemoryRows[1].sourceText").isEqualTo("Existing source")
        node("_embedded.translationMemoryRows[2].sourceText").isEqualTo("Manual phrase")
        node("_embedded.translationMemoryRows[3].sourceText").isEqualTo("Promoted source")
        node("_embedded.translationMemoryRows[4].sourceText").isEqualTo("Reviewed source")
      }
  }

  @Test
  fun `manual entry sharing source text with a virtual row renders on its own row`() {
    // Row-level pagination keeps stored manual entries on their own row even when they
    // share a source text with virtual rows. Stored rows are user-editable and unlabeled;
    // virtual rows stay read-only and carry the project key.
    val projectTmId = testData.projectTm.id
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$projectTmId/entries",
      TranslationMemoryEntryRequest().apply {
        sourceText = "Existing source"
        targetText = "Manual override translation"
        targetLanguageTag = "de"
      },
    ).andIsOk

    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$projectTmId/entries",
    ).andIsOk
      .andAssertThatJson {
        // 4 virtual rows (one per write-access project key) + 1 stored row for the manual
        // override on "Existing source" = 5 total rows.
        node("page.totalElements").isEqualTo(5)
        node("_embedded.translationMemoryRows").isArray.hasSize(5)
      }
  }

  @Test
  fun `project TM listing finds manual entries via target-text search`() {
    val projectTmId = testData.projectTm.id
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$projectTmId/entries",
      TranslationMemoryEntryRequest().apply {
        sourceText = "Manual phrase"
        targetText = "Frase manual unique"
        targetLanguageTag = "es"
      },
    ).andIsOk

    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$projectTmId/entries?search=unique",
    ).andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
        node("_embedded.translationMemoryRows[0].sourceText").isEqualTo("Manual phrase")
      }
  }

  @Test
  fun `comma-separated language filter returns entries for all specified languages`() {
    // Test data: sharedTm has "Hello world" in de+fr and "Thank you" in de only

    // de,fr → 2 groups; "Hello world" has 2 entries (de+fr), "Thank you" has 1 (de)
    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries?targetLanguageTag=de,fr",
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemoryRows").isArray.hasSize(2)
      }

    // fr only → still 2 groups; "Hello world" has 1 entry, "Thank you" has 0
    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries?targetLanguageTag=fr",
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemoryRows").isArray.hasSize(2)
      }
  }

  @Test
  fun `creates an entry`() {
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = "Good morning"
        targetText = "Guten Morgen"
        targetLanguageTag = "de"
      }
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      request,
    ).andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("sourceText").isEqualTo("Good morning")
        node("targetText").isEqualTo("Guten Morgen")
        node("targetLanguageTag").isEqualTo("de")
      }

    // Test data has 3 entries (Hello world de+fr, Thank you de); creating one more = 4
    val entries = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId)
    assertThat(entries).hasSize(4)
  }

  @Test
  fun `rejects blank source text`() {
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = "   "
        targetText = "Guten Morgen"
        targetLanguageTag = "de"
      }
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      request,
    ).andIsBadRequest
  }

  @Test
  fun `rejects oversized source text`() {
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = "x".repeat(TranslationMemoryEntry.MAX_TEXT_LENGTH + 1)
        targetText = "Guten Morgen"
        targetLanguageTag = "de"
      }
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      request,
    ).andIsBadRequest
  }

  @Test
  fun `updates an entry`() {
    // Pick the (Hello world, de) entry deterministically — findBy… has no ordering guarantee and
    // picking arbitrarily could collide with another existing entry on the targetLanguageTag we
    // set below.
    val entry =
      translationMemoryEntryRepository
        .findByTranslationMemoryId(sharedTmId)
        .first { it.sourceText == "Hello world" && it.targetLanguageTag == "de" }
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = entry.sourceText
        targetText = "Hallo Welt — updated"
        targetLanguageTag = "de"
      }
    performAuthPut(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/${entry.id}",
      request,
    ).andIsOk
      .andAssertThatJson {
        node("targetText").isEqualTo("Hallo Welt — updated")
      }
  }

  @Test
  fun `deletes an entry`() {
    val entry = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId).first()
    performAuthDelete(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/${entry.id}",
    ).andIsOk

    // Test data has 3 entries; after deleting one, 2 remain
    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId)).hasSize(2)
  }

  @Test
  fun `batch delete wipes whole groups for every representative entry`() {
    // sharedTm has 3 entries across 2 groups: "Hello world" (de+fr) and "Thank you" (de).
    // Passing one entry ID per group must remove the entire TU for each.
    val entries = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId)
    val helloEntry = entries.first { it.sourceText == "Hello world" && it.targetLanguageTag == "de" }
    val thankYouEntry = entries.first { it.sourceText == "Thank you" }

    performAuthDelete(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      DeleteMultipleTranslationMemoryEntriesRequest().apply {
        entryIds = setOf(helloEntry.id, thankYouEntry.id)
      },
    ).andIsOk

    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId)).isEmpty()
  }

  @Test
  fun `batch delete dedupes entries from the same group`() {
    // Two entries share the same "Hello world" source (de + fr). Passing both IDs must
    // still leave "Thank you" intact — the dedup protects us from double-issuing the SQL.
    val entries = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId)
    val helloDe = entries.first { it.sourceText == "Hello world" && it.targetLanguageTag == "de" }
    val helloFr = entries.first { it.sourceText == "Hello world" && it.targetLanguageTag == "fr" }

    performAuthDelete(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      DeleteMultipleTranslationMemoryEntriesRequest().apply {
        entryIds = setOf(helloDe.id, helloFr.id)
      },
    ).andIsOk

    val remaining = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId)
    assertThat(remaining).hasSize(1)
    assertThat(remaining.first().sourceText).isEqualTo("Thank you")
  }

  @Test
  fun `batch delete ignores entries from a different TM`() {
    // Mixing an entry from this TM with an entry from a different TM: only this TM's group is
    // deleted. Uses two shared TMs since project TMs store no entries anymore.
    val sharedEntry =
      translationMemoryEntryRepository
        .findByTranslationMemoryId(sharedTmId)
        .first { it.sourceText == "Hello world" && it.targetLanguageTag == "de" }
    val foreignEntry =
      translationMemoryEntryRepository
        .findByTranslationMemoryId(testData.sharedTmWithPenalty.id)
        .first()

    performAuthDelete(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      DeleteMultipleTranslationMemoryEntriesRequest().apply {
        entryIds = setOf(sharedEntry.id, foreignEntry.id)
      },
    ).andIsOk

    // "Hello world" group gone; "Thank you" stays
    val remaining = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId)
    assertThat(remaining).hasSize(1)
    assertThat(remaining.first().sourceText).isEqualTo("Thank you")
    // The other TM's entry is untouched — the endpoint only operates on the TM in the URL.
    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(testData.sharedTmWithPenalty.id))
      .isNotEmpty
  }

  @Test
  fun `member cannot batch delete entries`() {
    val entry = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId).first()
    userAccount = testData.orgMember
    performAuthDelete(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      DeleteMultipleTranslationMemoryEntriesRequest().apply { entryIds = setOf(entry.id) },
    ).andIsForbidden
  }

  @Test
  fun `returns 404 for entry from a different TM`() {
    val entry = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId).first()
    // Use a different TM id (projectTm) — the entry does not belong to it
    performAuthGet(
      "/v2/organizations/$orgId/translation-memories/${testData.projectTm.id}/entries/${entry.id}",
    ).andIsNotFound
  }

  @Test
  fun `does not allow create when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = "Bonjour"
        targetText = "Hallo"
        targetLanguageTag = "de"
      }
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      request,
    ).andIsBadRequest
  }

  // ---------- Bulk create (atomic multi-language POST) ----------

  private fun bulkRequest(
    source: String,
    vararg translations: Pair<String, String>,
  ) = CreateMultipleTranslationMemoryEntriesRequest().apply {
    sourceText = source
    this.translations =
      translations.map { (lang, text) ->
        CreateMultipleTranslationMemoryEntriesTranslationRequest().apply {
          targetLanguageTag = lang
          targetText = text
        }
      }
  }

  @Test
  fun `bulk create lands all target languages in one request`() {
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/multiple",
      bulkRequest("Bonjour", "de" to "Guten Morgen", "fr" to "Bonjour", "es" to "Hola"),
    ).andIsOk

    val newEntries =
      translationMemoryEntryRepository
        .findByTranslationMemoryId(sharedTmId)
        .filter { it.sourceText == "Bonjour" }
    assertThat(newEntries).hasSize(3)
    assertThat(newEntries.map { it.targetLanguageTag }).containsExactlyInAnyOrder("de", "fr", "es")
  }

  @Test
  fun `bulk create rejects duplicate target languages in one request`() {
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/multiple",
      bulkRequest("Bonjour", "de" to "Guten Morgen", "de" to "Conflict"),
    ).andIsBadRequest

    // Nothing landed — atomicity holds across validation failure
    val landed =
      translationMemoryEntryRepository
        .findByTranslationMemoryId(sharedTmId)
        .filter { it.sourceText == "Bonjour" }
    assertThat(landed).isEmpty()
  }

  @Test
  fun `bulk create rolls back if one translation is invalid`() {
    // One target text exceeds the per-entry size cap — the whole request must fail and leave
    // the other (valid) translations unpersisted. Without atomicity the FE would see only the
    // first language land, then a generic error for the rest.
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/multiple",
      bulkRequest(
        "Bonjour",
        "de" to "Guten Morgen",
        "fr" to "x".repeat(TranslationMemoryEntry.MAX_TEXT_LENGTH + 1),
      ),
    ).andIsBadRequest

    val landed =
      translationMemoryEntryRepository
        .findByTranslationMemoryId(sharedTmId)
        .filter { it.sourceText == "Bonjour" }
    assertThat(landed).isEmpty()
  }

  @Test
  fun `bulk create requires at least one translation`() {
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/multiple",
      bulkRequest("Bonjour"),
    ).andIsBadRequest
  }

  @Test
  fun `bulk create is forbidden when feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/multiple",
      bulkRequest("Bonjour", "de" to "Guten Morgen"),
    ).andIsBadRequest
  }

  @Test
  fun `member cannot bulk create entries`() {
    userAccount = testData.orgMember
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/multiple",
      bulkRequest("Bonjour", "de" to "Guten Morgen"),
    ).andIsForbidden
  }

  // ---------- Long-segment / hash-expression index sanity ----------
  // These guard `ix_tm_entry_tm_source` — a btree on `(translation_memory_id, md5(source_text))`.
  // A plain btree over raw source_text would hit Postgres' ~2704 B per-row limit on long CJK
  // segments. We exercise both the insert path (which lands the index entry) and the lookup
  // path (`findByTranslationMemoryIdAndSourceTexts`) so a regression to a non-hash index would
  // fail loudly here.

  @Test
  fun `accepts and reads back a 5000-char ASCII source text`() {
    val longSource = "x".repeat(5_000)
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = longSource
        targetText = "ok"
        targetLanguageTag = "de"
      }
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      request,
    ).andIsOk

    val found =
      translationMemoryEntryRepository.findByTranslationMemoryIdAndSourceTexts(
        translationMemoryId = sharedTmId,
        sourceTexts = arrayOf(longSource),
        targetLanguageTags = null,
      )
    assertThat(found).hasSize(1)
    assertThat(found.first().sourceText).isEqualTo(longSource)
  }

  @Test
  fun `accepts and reads back an 8000-char CJK source text`() {
    // ~24 KB in UTF-8 — would overflow a plain `(translation_memory_id, source_text)` btree
    // well before reaching this length. The md5 expression index doesn't care.
    val longCjk = "中".repeat(8_000)
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = longCjk
        targetText = "ok"
        targetLanguageTag = "de"
      }
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      request,
    ).andIsOk

    val found =
      translationMemoryEntryRepository.findByTranslationMemoryIdAndSourceTexts(
        translationMemoryId = sharedTmId,
        sourceTexts = arrayOf(longCjk),
        targetLanguageTags = null,
      )
    assertThat(found).hasSize(1)
    assertThat(found.first().sourceText).isEqualTo(longCjk)
  }

  @Test
  fun `entry lookup ignores source texts that are not in the TM`() {
    // Collision guard: the md5 predicate may theoretically (~2^-128) line up against an
    // unrelated source_text; the literal `source_text = any(...)` filter keeps us correct.
    val matches =
      translationMemoryEntryRepository.findByTranslationMemoryIdAndSourceTexts(
        translationMemoryId = sharedTmId,
        sourceTexts = arrayOf("Hello world", "no such source"),
        targetLanguageTags = null,
      )
    assertThat(matches).isNotEmpty
    assertThat(matches.map { it.sourceText }).allMatch { it == "Hello world" }
  }

  // ---------- Permission tests (org MEMBER cannot mutate) ----------

  @Test
  fun `member cannot create entry`() {
    userAccount = testData.orgMember
    val request =
      TranslationMemoryEntryRequest().apply {
        sourceText = "Hi"
        targetText = "Hallo"
        targetLanguageTag = "de"
      }
    performAuthPost(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries",
      request,
    ).andIsForbidden
  }

  @Test
  fun `member cannot update entry`() {
    val entry = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId).first()
    userAccount = testData.orgMember
    performAuthPut(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/${entry.id}",
      TranslationMemoryEntryRequest().apply {
        sourceText = entry.sourceText
        targetText = "tampered"
        targetLanguageTag = "de"
      },
    ).andIsForbidden
  }

  @Test
  fun `member cannot delete entry`() {
    val entry = translationMemoryEntryRepository.findByTranslationMemoryId(sharedTmId).first()
    userAccount = testData.orgMember
    performAuthDelete(
      "/v2/organizations/$orgId/translation-memories/$sharedTmId/entries/${entry.id}",
    ).andIsForbidden
  }

  @Test
  fun `member can list entries`() {
    userAccount = testData.orgMember
    performAuthGet("/v2/organizations/$orgId/translation-memories/$sharedTmId/entries").andIsOk
  }
}
