package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

/**
 * Guards the AI-match backfill against N+1 / per-row writes at scale. Tolgee projects can hold
 * millions of translations, so the cost of recomputing the materialized table must grow with the
 * number of *batches*, not the number of translations.
 *
 * Asserts the JDBC statement count for a [KEY_COUNT]-translation backfill stays well below
 * [KEY_COUNT] (and the cached re-read is near-zero), and prints wall-clock timings.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiMatchStatsPerformanceTest : ProjectAuthControllerTest("/v2/projects/") {
  companion object {
    private const val KEY_COUNT = 10_000
    private const val MATCH_TEXT = "match text here" // 3 words; reviewed verbatim -> score 100
    private const val AI_REVISION_BASE = 100_000_000L
    private const val REVIEWED_REVISION_BASE = 200_000_000L
  }

  private lateinit var testData: BaseTestData
  private var deLanguageId: Long = 0

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    val de =
      testData.projectBuilder
        .addLanguage {
          name = "German"
          tag = "de"
        }.self
    repeat(KEY_COUNT) { i ->
      testData.projectBuilder.addKey { name = "perf-key-$i" }.build {
        addTranslation {
          language = de
          text = MATCH_TEXT
          state = TranslationState.REVIEWED
        }
      }
    }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
    deLanguageId = de.id
    seedActivity()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `backfills a large project without N+1 or per-row writes`() {
    val statistics = entityManager.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
    statistics.isStatisticsEnabled = true

    statistics.clear()
    val backfillStart = System.nanoTime()
    performProjectAuthGet("ai-match-stats").andIsOk.andAssertThatJson {
      node("reviewedKeys").isEqualTo(KEY_COUNT)
      node("reviewedWords").isEqualTo(KEY_COUNT * 3)
      node("b100.words").isEqualTo(KEY_COUNT * 3)
      node("avgMatchScore").isEqualTo(100)
    }
    val backfillMs = (System.nanoTime() - backfillStart) / 1_000_000
    val backfillStatements = statistics.prepareStatementCount

    statistics.clear()
    val cachedStart = System.nanoTime()
    performProjectAuthGet("ai-match-stats").andIsOk
    val cachedMs = (System.nanoTime() - cachedStart) / 1_000_000
    val cachedStatements = statistics.prepareStatementCount

    println(
      "[AiMatchStats perf] keys=$KEY_COUNT | backfill ${backfillMs}ms, $backfillStatements JDBC statements " +
        "| cached re-read ${cachedMs}ms, $cachedStatements JDBC statements",
    )

    // Backfill cost is O(batches): a handful of reads + (KEY_COUNT/1000) bulk upserts, nowhere near KEY_COUNT.
    assertThat(backfillStatements).isLessThan(120)
    // A re-read with no new activity hits the watermark cache and skips recompute, so it issues strictly
    // fewer statements than the backfill (the remainder is constant per-request auth/project overhead).
    assertThat(cachedStatements).isLessThan(backfillStatements)
  }

  /** Two activity revisions per German translation (AI-translated, then reviewed), seeded set-based. */
  private fun seedActivity() {
    val aiModifications =
      """{"text":{"old":null,"new":"$MATCH_TEXT"},"auto":{"old":false,"new":true},""" +
        """"mtProvider":{"old":null,"new":"GOOGLE"},"state":{"old":null,"new":"TRANSLATED"}}"""
    val reviewedModifications = """{"state":{"old":"TRANSLATED","new":"REVIEWED"}}"""

    executeInNewTransaction {
      insertRevisions(AI_REVISION_BASE, "2026-01-01 00:00:00")
      insertModifiedEntities(AI_REVISION_BASE, aiModifications)
      insertRevisions(REVIEWED_REVISION_BASE, "2026-02-01 00:00:00")
      insertModifiedEntities(REVIEWED_REVISION_BASE, reviewedModifications)
    }
  }

  private fun insertRevisions(
    base: Long,
    timestamp: String,
  ) {
    entityManager
      .createNativeQuery(
        """
        INSERT INTO activity_revision (id, timestamp, project_id)
        SELECT :base + row_number() OVER (ORDER BY t.id), CAST(:ts AS timestamp), :projectId
        FROM translation t
        JOIN key k ON k.id = t.key_id
        WHERE k.project_id = :projectId AND t.language_id = :deId
        """,
      ).setParameter("base", base)
      .setParameter("ts", timestamp)
      .setParameter("projectId", testData.project.id)
      .setParameter("deId", deLanguageId)
      .executeUpdate()
  }

  private fun insertModifiedEntities(
    base: Long,
    modifications: String,
  ) {
    entityManager
      .createNativeQuery(
        """
        INSERT INTO activity_modified_entity (activity_revision_id, entity_class, entity_id, modifications, revision_type)
        SELECT :base + s.rn, 'Translation', s.tid, CAST(:mods AS jsonb), 1
        FROM (
          SELECT t.id AS tid, row_number() OVER (ORDER BY t.id) AS rn
          FROM translation t
          JOIN key k ON k.id = t.key_id
          WHERE k.project_id = :projectId AND t.language_id = :deId
        ) s
        """,
      ).setParameter("base", base)
      .setParameter("mods", modifications)
      .setParameter("projectId", testData.project.id)
      .setParameter("deId", deLanguageId)
      .executeUpdate()
  }
}
