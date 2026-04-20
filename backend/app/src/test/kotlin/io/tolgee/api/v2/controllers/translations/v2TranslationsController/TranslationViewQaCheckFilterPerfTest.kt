package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.CleanDbBeforeClass
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

/**
 * Diagnostic perf test isolated for `filterQaCheckType` (EMPTY_TRANSLATION).
 *
 * Enables `useQaChecks` on the project and inserts one OPEN `EMPTY_TRANSLATION` QA issue on
 * lang-1's translation for every [QA_FREQUENCY]-th key. No tags, outdated flags, or suggestions.
 *
 * `@Disabled` by default. Run manually:
 *
 * ```
 * ./gradlew :server-app:test --tests "io.tolgee.api.v2.controllers.translations.v2TranslationsController.TranslationViewQaCheckFilterPerfTest"
 * ```
 */
@Disabled("Diagnostic perf test — run manually when benchmarking the Translation View query")
@CleanDbBeforeClass
class TranslationViewQaCheckFilterPerfTest : AbstractPartialFixturePerfTestBase() {
  companion object {
    const val QA_FREQUENCY = 5 // ~2000 keys
  }

  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  override fun configureProject(projectBuilder: ProjectBuilder) {
    projectBuilder.self.useQaChecks = true
  }

  override fun setupFilterSpecificFixture() {
    val lang1Id = languages.first().id
    val qaKeyNames = (1..KEY_COUNT).filter { it % QA_FREQUENCY == 0 }.map { "key %05d".format(it) }

    val qaTranslations =
      entityManager
        .createQuery(
          """
          FROM Translation t
          WHERE t.language.id = :langId
            AND t.key.project.id = :pid
            AND t.key.name IN :names
          """.trimIndent(),
          Translation::class.java,
        ).setParameter("langId", lang1Id)
        .setParameter("pid", projectId)
        .setParameter("names", qaKeyNames)
        .resultList

    for (t in qaTranslations) {
      val issue =
        TranslationQaIssue(
          type = QaCheckType.EMPTY_TRANSLATION,
          message = QaIssueMessage.QA_EMPTY_TRANSLATION,
          state = QaIssueState.OPEN,
          translation = t,
        )
      entityManager.persist(issue)
    }
    entityManager.flush()
    entityManager.clear()
    logger.info("  inserted ${qaTranslations.size} QA issues")
  }

  @Test
  fun `filterQaCheckType=EMPTY_TRANSLATION`() {
    val expected = (KEY_COUNT / QA_FREQUENCY).toLong()
    val title = "filterQaCheckType=EMPTY_TRANSLATION (~$expected matches)"
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
                filterQaCheckType = listOf(QaCheckType.EMPTY_TRANSLATION)
              },
          )
        assertThat(result.totalElements).isEqualTo(expected)
      }
    }
  }
}
