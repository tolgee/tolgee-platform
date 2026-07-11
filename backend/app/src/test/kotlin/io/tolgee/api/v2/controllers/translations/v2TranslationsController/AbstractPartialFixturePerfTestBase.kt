package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.repository.LanguageRepository
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired

/**
 * Shared base for all diagnostic "partial fixture" perf tests.
 *
 * The base sets up a project with [LANGUAGE_COUNT] languages and inserts [KEY_COUNT] keys, each
 * translated only in the first [TRANSLATED_LANGUAGE_COUNT] languages. Subclasses then implement
 * [setupFilterSpecificFixture] to add whatever extra data their filter requires (tags, outdated
 * translations, QA issues, suggestions, …).
 *
 * This split lets us **isolate each filter in its own class**: the database for a given test
 * class contains only the rows relevant to the filter being measured, so query plans, cache, and
 * timing are not polluted by unrelated fixture data. Running one class is enough to reproduce and
 * diagnose a perf problem for one specific filter.
 *
 * All concrete subclasses should:
 *  - be annotated `@Disabled` (diagnostic — not for CI)
 *  - be annotated `@CleanDbBeforeClass`
 *  - implement [setupFilterSpecificFixture] to populate filter-specific data
 *  - optionally override [configureProject] to flip project-level flags (e.g. `useQaChecks`)
 */
@Suppress("LateinitVarOverridesLateinitVar")
abstract class AbstractPartialFixturePerfTestBase :
  AbstractSpringTest(),
  Logging {
  companion object {
    const val KEY_COUNT = 10_000
    const val LANGUAGE_COUNT = 20
    const val TRANSLATED_LANGUAGE_COUNT = 2
    const val PAGE_SIZE = 20
    const val WARMUP_RUNS = 1
    const val MEASURED_RUNS = 3
  }

  @Autowired
  protected lateinit var languageRepository: LanguageRepository

  protected var projectId: Long = 0L
  protected lateinit var languages: List<Language>

  @BeforeAll
  fun setupFixture() {
    val totalSetup = System.currentTimeMillis()
    val keyCreateMs = measureMs { createProjectAndKeys() }
    val extraMs = measureMs { executeInNewTransaction { setupFilterSpecificFixture() } }
    val totalMs = System.currentTimeMillis() - totalSetup

    logger.info("=".repeat(72))
    logger.info("${this::class.simpleName} FIXTURE READY")
    logger.info("  Project id=$projectId")
    logger.info("  Languages: $LANGUAGE_COUNT (only first $TRANSLATED_LANGUAGE_COUNT translated)")
    logger.info("  Keys: $KEY_COUNT")
    logger.info("  Translations: ${KEY_COUNT * TRANSLATED_LANGUAGE_COUNT}")
    logger.info("  Setup time: ${keyCreateMs}ms (keys) / ${extraMs}ms (filter-specific) / ${totalMs}ms (total)")
    logger.info("=".repeat(72))
  }

  /**
   * Override in subclasses to populate the filter-specific fixture (tags, outdated flags, QA
   * issues, suggestions, …). Default: no-op.
   *
   * Called inside a transaction right after keys and their translations are inserted.
   */
  protected open fun setupFilterSpecificFixture() {}

  /**
   * Hook for subclasses to flip project-level flags (e.g. `self.useQaChecks = true`) before the
   * project is saved.
   */
  protected open fun configureProject(projectBuilder: ProjectBuilder) {}

  private fun createProjectAndKeys() {
    val testData = BaseTestData(userName = "perf-user", projectName = "perf-project")
    testData.projectBuilder.apply {
      configureProject(this)
      for (i in 2..LANGUAGE_COUNT) {
        addLanguage {
          name = "Language $i"
          originalName = "Language $i"
          tag = "lang-$i"
        }
      }
      for (i in 1..KEY_COUNT) {
        addKey {
          name = "key %05d".format(i)
        }.build {
          for (langIdx in 0 until TRANSLATED_LANGUAGE_COUNT) {
            val langBuilder =
              testData.root.data.projects[0]
                .data.languages[langIdx]
            addTranslation {
              language = langBuilder.self
              text = "Translation of key %05d in ${langBuilder.self.tag}".format(i)
              state = TranslationState.TRANSLATED
            }
          }
        }
      }
    }
    testDataService.saveTestData(testData.root)
    projectId = testData.project.id
    languages = languageRepository.findAllByProjectId(projectId).sortedBy { it.id }
  }

  protected fun languageDtos(langs: List<Language>): Set<LanguageDto> =
    languageService.dtosFromEntities(langs, projectId).toSet()

  protected fun liftStatementTimeout() {
    entityManager.createNativeQuery("SET LOCAL statement_timeout TO 600000").executeUpdate()
  }

  protected fun measureScenario(
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

  protected fun measureMs(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
  }
}
