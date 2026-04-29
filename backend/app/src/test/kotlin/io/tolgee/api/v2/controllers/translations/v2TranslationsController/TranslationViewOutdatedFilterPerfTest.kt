package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.CleanDbBeforeClass
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

/**
 * Diagnostic perf test isolated for `filterOutdatedLanguage`.
 *
 * Marks `Translation.outdated = true` on lang-1's translation for every [OUTDATED_FREQUENCY]-th
 * key on top of the base fixture. No tags, QA issues, or suggestions are populated.
 *
 * `@Disabled` by default. Run manually:
 *
 * ```
 * ./gradlew :server-app:test --tests "io.tolgee.api.v2.controllers.translations.v2TranslationsController.TranslationViewOutdatedFilterPerfTest"
 * ```
 */
@Disabled("Diagnostic perf test — run manually when benchmarking the Translation View query")
@CleanDbBeforeClass
class TranslationViewOutdatedFilterPerfTest : AbstractPartialFixturePerfTestBase() {
  companion object {
    const val OUTDATED_FREQUENCY = 5 // ~2000 keys
  }

  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  override fun setupFilterSpecificFixture() {
    val lang1Id = languages.first().id
    val updated =
      entityManager
        .createNativeQuery(
          """
          UPDATE translation
          SET outdated = true
          WHERE language_id = :langId
            AND key_id IN (
              SELECT id FROM key
              WHERE project_id = :pid
                AND CAST(SUBSTRING(name FROM 5) AS INTEGER) % :freq = 0
            )
          """.trimIndent(),
        ).setParameter("langId", lang1Id)
        .setParameter("pid", projectId)
        .setParameter("freq", OUTDATED_FREQUENCY)
        .executeUpdate()
    logger.info("  marked $updated translations as outdated")
  }

  @Test
  fun `filterOutdatedLanguage on lang-1`() {
    val expected = (KEY_COUNT / OUTDATED_FREQUENCY).toLong()
    val outdatedLang = languages.first()
    val title = "filterOutdatedLanguage=${outdatedLang.tag} (~$expected matches)"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = GetTranslationsParams().apply { filterOutdatedLanguage = listOf(outdatedLang.tag) },
          )
        assertThat(result.totalElements).isEqualTo(expected)
      }
    }
  }
}
