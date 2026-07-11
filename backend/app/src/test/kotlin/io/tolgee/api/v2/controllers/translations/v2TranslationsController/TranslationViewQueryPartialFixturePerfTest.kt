package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.CleanDbBeforeClass
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

/**
 * Diagnostic perf test for the partial-translation case — **state/text-only filters**.
 *
 * The fixture is intentionally lopsided to mirror the typical production workload: every key has
 * translations for two "core" languages (`lang-1`, `lang-2`) and the remaining 18 languages are
 * empty. This is the case where filters like `filterUntranslatedAny`, `filterUntranslatedInLang`,
 * and homogeneous `filterState=lang,UNTRANSLATED` across all selected languages all match almost
 * every key — i.e. the typical "early-stage i18n" project shape.
 *
 * Filter tests that need additional fixtures (tags, outdated flags, QA issues, suggestions) live
 * in their own dedicated classes next to this one, each isolated so the database contains only
 * the rows relevant to that filter. See [AbstractPartialFixturePerfTestBase] for rationale.
 *
 * `@Disabled` by default. Run manually:
 *
 * ```
 * ./gradlew :server-app:test --tests "io.tolgee.api.v2.controllers.translations.v2TranslationsController.TranslationViewQueryPartialFixturePerfTest"
 * ```
 */
@Disabled("Diagnostic perf test — run manually when benchmarking the Translation View query")
@CleanDbBeforeClass
class TranslationViewQueryPartialFixturePerfTest : AbstractPartialFixturePerfTestBase() {
  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  /**
   * The user-visible "show all untranslated" filter. The frontend sends one
   * `filterState=lang,UNTRANSLATED` per selected language; the refactor's homogeneous detection
   * collapses these into a single combined predicate for the data query.
   */
  @Test
  fun `filterState=lang,UNTRANSLATED across all 20 languages (homogeneous)`() {
    val title = "filterState=lang,UNTRANSLATED across $LANGUAGE_COUNT langs (homogeneous)"
    val params =
      GetTranslationsParams(
        filterState = languages.map { "${it.tag},UNTRANSLATED" },
      )
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = params,
          )
        // Every key has 18 untranslated languages → all 10k keys match
        assertThat(result.totalElements).isEqualTo(KEY_COUNT.toLong())
      }
    }
  }

  @Test
  fun `filterUntranslatedAny across all 20 languages`() {
    val title = "filterUntranslatedAny across $LANGUAGE_COUNT langs"
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
        assertThat(result.totalElements).isEqualTo(KEY_COUNT.toLong())
      }
    }
  }

  @Test
  fun `filterUntranslatedInLang for a missing language`() {
    val title = "filterUntranslatedInLang=<missing-lang>"
    val missingLang = languages.last() // lang-20 is not translated
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = GetTranslationsParams().apply { filterUntranslatedInLang = missingLang.tag },
          )
        assertThat(result.totalElements).isEqualTo(KEY_COUNT.toLong())
      }
    }
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
}
