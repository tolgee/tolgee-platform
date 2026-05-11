package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.CleanDbBeforeClass
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.key.Key
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

/**
 * Diagnostic perf test isolated for `filterHasSuggestionsInLang`.
 *
 * Inserts one ACTIVE [TranslationSuggestion] on lang-1 for every [SUGGESTION_FREQUENCY]-th key
 * on top of the base fixture. No tags, outdated flags, or QA issues.
 *
 * `@Disabled` by default. Run manually:
 *
 * ```
 * ./gradlew :server-app:test --tests "io.tolgee.api.v2.controllers.translations.v2TranslationsController.TranslationViewSuggestionsFilterPerfTest"
 * ```
 */
@Disabled("Diagnostic perf test — run manually when benchmarking the Translation View query")
@CleanDbBeforeClass
class TranslationViewSuggestionsFilterPerfTest : AbstractPartialFixturePerfTestBase() {
  companion object {
    const val SUGGESTION_FREQUENCY = 5 // ~2000 keys
  }

  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  override fun setupFilterSpecificFixture() {
    val suggestionKeyNames = (1..KEY_COUNT).filter { it % SUGGESTION_FREQUENCY == 0 }.map { "key %05d".format(it) }

    val suggestionKeys =
      entityManager
        .createQuery("FROM Key k WHERE k.project.id = :pid AND k.name IN :names", Key::class.java)
        .setParameter("pid", projectId)
        .setParameter("names", suggestionKeyNames)
        .resultList

    val managedProject = entityManager.find(Project::class.java, projectId)
    val lang1 = entityManager.find(Language::class.java, languages.first().id)

    for (k in suggestionKeys) {
      val suggestion =
        TranslationSuggestion(
          project = managedProject,
          language = lang1,
          author = null,
          translation = "suggestion for ${k.name}",
          state = TranslationSuggestionState.ACTIVE,
        ).apply { this.key = k }
      entityManager.persist(suggestion)
    }
    entityManager.flush()
    entityManager.clear()
    logger.info("  inserted ${suggestionKeys.size} suggestions")
  }

  @Test
  fun `filterHasSuggestionsInLang on lang-1`() {
    val expected = (KEY_COUNT / SUGGESTION_FREQUENCY).toLong()
    val suggestionLang = languages.first()
    val title = "filterHasSuggestionsInLang=${suggestionLang.tag} (~$expected matches)"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = GetTranslationsParams().apply { filterHasSuggestionsInLang = listOf(suggestionLang.tag) },
          )
        assertThat(result.totalElements).isEqualTo(expected)
      }
    }
  }
}
