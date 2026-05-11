package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.AbstractSpringTest
import io.tolgee.CleanDbBeforeClass
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.LanguageRepository
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

/**
 * Diagnostic performance test for the Translation View query.
 *
 * Runs `TranslationViewDataProvider.getData(...)` over a large fixture (10 000 keys × 20 languages
 * = 200 000 translations) and measures query execution time across several scenarios.
 *
 * **Disabled by default** — not intended for CI. Run manually when investigating or
 * benchmarking the Translation View query:
 *
 * ```
 * ./gradlew :server-app:test --tests "io.tolgee.api.v2.controllers.translations.v2TranslationsController.TranslationViewQueryPerfTest"
 * ```
 *
 * Each test method warms up the JIT / connection pool / buffer cache (1 untimed run) and then
 * takes the median of 3 timed runs to reduce noise. The warmup time is captured in the log
 * because it represents the cold-cache cost — what real users hit when the page is first
 * loaded after a server restart, or when accessing a project whose data has been evicted from
 * PostgreSQL's shared buffers.
 *
 * The fixture is populated **once** in `@BeforeAll` (the class is `@CleanDbBeforeClass`, so the
 * `CleanDbTestListener` does NOT truncate before each test method) and reused across all tests.
 *
 * ## Representative numbers
 *
 * On a developer laptop with dockerized PostgreSQL, the current implementation produces:
 *
 * | Scenario                                 | Cold (warmup) | Warm (median 3) |
 * |------------------------------------------|---------------|-----------------|
 * | Page load 20 keys × 20 langs             | ~70 ms        | ~65 ms          |
 * | Page load 20 keys × 15 langs             | ~70 ms        | ~55 ms          |
 * | Full-text search across 20 langs         | ~280 ms       | ~220 ms         |
 * | filterUntranslatedAny across 20 langs    | ~150 ms       | ~70 ms          |
 *
 * These numbers are a rough reference — PostgreSQL page cache state, host load, and JVM JIT
 * behavior can all move them by a factor of 2–3. If you see dramatic deviations (seconds
 * instead of hundreds of milliseconds), the regression is probably in the query, not the
 * environment.
 */
@Suppress("LateinitVarOverridesLateinitVar")
@Disabled("Diagnostic perf test — run manually when benchmarking the Translation View query")
@CleanDbBeforeClass
class TranslationViewQueryPerfTest :
  AbstractSpringTest(),
  Logging {
  companion object {
    const val KEY_COUNT = 10_000
    const val LANGUAGE_COUNT = 20
    const val PAGE_SIZE = 20
    const val WARMUP_RUNS = 1
    const val MEASURED_RUNS = 3
  }

  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  @Autowired
  private lateinit var languageRepository: LanguageRepository

  private var projectId: Long = 0L
  private lateinit var languages: List<Language>

  @BeforeAll
  fun setupLargeDataset() {
    val totalSetup = System.currentTimeMillis()
    val baseSetupMs =
      measureMs {
        executeInNewTransaction { createProjectAndLanguages() }
      }
    val keyCreateMs =
      measureMs {
        executeInNewTransaction { bulkCreateKeysAndTranslations() }
      }
    val totalMs = System.currentTimeMillis() - totalSetup

    logger.info("=".repeat(72))
    logger.info("PERF TEST FIXTURE READY")
    logger.info("  Project id=$projectId")
    logger.info("  Languages: $LANGUAGE_COUNT")
    logger.info("  Keys: $KEY_COUNT")
    logger.info("  Translations: ${KEY_COUNT * LANGUAGE_COUNT}")
    logger.info("  Project+languages setup: ${baseSetupMs}ms")
    logger.info("  Key + translation insert time: ${keyCreateMs}ms")
    logger.info("  Total setup time: ${totalMs}ms")
    logger.info("=".repeat(72))
  }

  @Test
  fun `page load with default 20 keys and all 20 languages`() {
    val title = "Page load: first $PAGE_SIZE keys × $LANGUAGE_COUNT languages"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = GetTranslationsParams(),
          )
        assertThat(result.content).hasSize(PAGE_SIZE)
        assertThat(result.totalElements).isEqualTo(KEY_COUNT.toLong())
      }
    }
  }

  @Test
  fun `full-text search across all 20 languages`() {
    // Full-text search is a worst-case filter: it LIKEs the key name, namespace name,
    // description, and every translation row across the selected languages — an EXISTS
    // subquery has to scan the translation table for each candidate key. Included here so
    // regressions in the search path are visible alongside the simpler page-load numbers.
    val title = "Full-text search '500' across $LANGUAGE_COUNT languages"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params =
              GetTranslationsParams().apply {
                search = "500"
              },
          )
        // Verify we got SOMETHING (the substring "500" appears in keyName "key 00500" and the
        // generated translation text). The exact count is irrelevant for the perf test.
        assertThat(result.totalElements).isGreaterThan(0L)
      }
    }
  }

  @Test
  fun `select 15 languages — above the default translationsViewLanguagesLimit`() {
    val title = "Page load: first $PAGE_SIZE keys × 15 languages"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val subset = languageDtos(languages.take(15))
        assertThat(subset).hasSize(15)
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = subset,
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = GetTranslationsParams(),
          )
        assertThat(result.content).hasSize(PAGE_SIZE)
        // Each returned key has exactly 15 language entries
        assertThat(result.content.first().translations).hasSize(15)
      }
    }
  }

  @Test
  fun `sort by translation text ASC across all 20 languages`() {
    // Sorting by a translation-text column uses a scalar correlated subquery per candidate row
    // (see QueryBase.scalarTranslationText). This exercises the ORDER BY + LIMIT path across
    // the full key set — each of the ~10 000 candidate keys evaluates the subquery once.
    val sortLang = languages.first().tag
    val title = "Sort by translations.$sortLang.text ASC × $LANGUAGE_COUNT languages"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable =
              PageRequest.of(
                0,
                PAGE_SIZE,
                org.springframework.data.domain.Sort.by(
                  org.springframework.data.domain.Sort.Order
                    .asc("translations.$sortLang.text"),
                  org.springframework.data.domain.Sort.Order
                    .asc("keyName"),
                ),
              ),
            params = GetTranslationsParams(),
          )
        assertThat(result.content).hasSize(PAGE_SIZE)
        assertThat(result.totalElements).isEqualTo(KEY_COUNT.toLong())
      }
    }
  }

  @Test
  fun `sort by translation text DESC across all 20 languages`() {
    val sortLang = languages.first().tag
    val title = "Sort by translations.$sortLang.text DESC × $LANGUAGE_COUNT languages"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable =
              PageRequest.of(
                0,
                PAGE_SIZE,
                org.springframework.data.domain.Sort.by(
                  org.springframework.data.domain.Sort.Order
                    .desc("translations.$sortLang.text"),
                  org.springframework.data.domain.Sort.Order
                    .asc("keyName"),
                ),
              ),
            params = GetTranslationsParams(),
          )
        assertThat(result.content).hasSize(PAGE_SIZE)
        assertThat(result.totalElements).isEqualTo(KEY_COUNT.toLong())
      }
    }
  }

  @Test
  fun `filter untranslated-any across all 20 languages`() {
    // The fixture creates a translation for every (key, language) combo, so this must return 0.
    // Intentionally exercises the "untranslated any" path across a large language set, which
    // historically has been a pathological case for the query builder.
    val title = "filterUntranslatedAny across $LANGUAGE_COUNT languages"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = GetTranslationsParams().apply { filterUntranslatedAny = true },
          )
        assertThat(result.totalElements).isEqualTo(0L)
      }
    }
  }

  // ─── helpers ────────────────────────────────────────────────────────────────────────────

  /**
   * Sets up project, user, organization, permission, and 20 languages via the test data builder.
   * Cheap (~20 entities), so we use the high-level builder rather than raw SQL.
   */
  private fun createProjectAndLanguages() {
    val testData = BaseTestData(userName = "perf-user", projectName = "perf-project")
    // Base sets up English; add 19 more languages so we have 20 total.
    testData.projectBuilder.apply {
      for (i in 2..LANGUAGE_COUNT) {
        addLanguage {
          name = "Language $i"
          originalName = "Language $i"
          tag = "lang-$i"
        }
      }
    }
    testDataService.saveTestData(testData.root)
    projectId = testData.project.id
    languages = languageRepository.findAllByProjectId(projectId).sortedBy { it.id }
    assertThat(languages).hasSize(LANGUAGE_COUNT)
  }

  /**
   * Bulk insert keys and translations using JPA persist with periodic flush+clear. This is
   * ~10x slower than raw SQL but doesn't need to know the schema.
   *
   * Avoids services entirely (no activity logging, no events, no validation).
   */
  private fun bulkCreateKeysAndTranslations() {
    val batchSize = 500
    var managedProject = entityManager.find(Project::class.java, projectId)
    var managedLanguages = languages.map { entityManager.find(Language::class.java, it.id) }

    val pendingKeys = ArrayList<Key>(batchSize)
    var insertedKeys = 0

    for (i in 1..KEY_COUNT) {
      val key =
        Key(name = "key %05d".format(i)).apply {
          this.project = managedProject
        }
      entityManager.persist(key)
      pendingKeys.add(key)

      if (pendingKeys.size >= batchSize) {
        // Flush keys before persisting translations to ensure key IDs are assigned
        entityManager.flush()
        for (k in pendingKeys) {
          for (language in managedLanguages) {
            val translation =
              Translation(text = "Translation of ${k.name} in ${language.tag}").apply {
                this.key = k
                this.language = language
                this.state = TranslationState.TRANSLATED
              }
            entityManager.persist(translation)
          }
        }
        entityManager.flush()
        entityManager.clear()
        insertedKeys += pendingKeys.size
        pendingKeys.clear()
        // Re-attach managed references after clear
        managedProject = entityManager.find(Project::class.java, projectId)
        managedLanguages = languages.map { entityManager.find(Language::class.java, it.id) }
        if (insertedKeys % 2000 == 0) {
          logger.info("  inserted $insertedKeys keys / ${insertedKeys * LANGUAGE_COUNT} translations")
        }
      }
    }
    if (pendingKeys.isNotEmpty()) {
      entityManager.flush()
      for (k in pendingKeys) {
        for (language in managedLanguages) {
          val translation =
            Translation(text = "Translation of ${k.name} in ${language.tag}").apply {
              this.key = k
              this.language = language
              this.state = TranslationState.TRANSLATED
            }
          entityManager.persist(translation)
        }
      }
      entityManager.flush()
      entityManager.clear()
      insertedKeys += pendingKeys.size
    }

    logger.info("  TOTAL inserted: $insertedKeys keys, ${insertedKeys * LANGUAGE_COUNT} translations")
    // Re-load `languages` from the current persistence context for use by test methods
    languages = languageRepository.findAllByProjectId(projectId).sortedBy { it.id }
  }

  private fun languageDtos(langs: List<Language>): Set<LanguageDto> =
    languageService.dtosFromEntities(langs, projectId).toSet()

  /**
   * `CleanDbTestListener` sets `statement_timeout = 5000` (5s) on the connection it uses for
   * table cleanup, and that setting can leak into the pool. We lift it per-test transaction
   * to 10 minutes so that a query regression manifests as a slow run rather than a timeout
   * with no measurement.
   */
  private fun liftStatementTimeout() {
    entityManager.createNativeQuery("SET LOCAL statement_timeout TO 600000").executeUpdate()
  }

  private fun measureScenario(
    title: String,
    block: () -> Unit,
  ) {
    logger.info("─".repeat(72))
    logger.info("SCENARIO: $title")

    repeat(WARMUP_RUNS) {
      val warmup = measureMs(block)
      logger.info("  warmup: ${warmup}ms")
    }

    val timings = ArrayList<Long>(MEASURED_RUNS)
    repeat(MEASURED_RUNS) { i ->
      val ms = measureMs(block)
      timings.add(ms)
      logger.info("  run #${i + 1}: ${ms}ms")
    }

    val sorted = timings.sorted()
    val median = sorted[sorted.size / 2]
    val min = sorted.first()
    val max = sorted.last()

    logger.info(">>> RESULT [$title]: median=${median}ms min=${min}ms max=${max}ms")
    logger.info("─".repeat(72))
  }

  private inline fun measureMs(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
  }
}
