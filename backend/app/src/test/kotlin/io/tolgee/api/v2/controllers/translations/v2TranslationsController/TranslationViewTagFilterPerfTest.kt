package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.CleanDbBeforeClass
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Tag
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

/**
 * Diagnostic perf test isolated for `filterTag` / `filterNoTag`.
 *
 * Adds a single tag `tag-common` to every [TAG_FREQUENCY]-th key on top of the base fixture. No
 * outdated flags, QA issues, or suggestions are populated — this class measures only the cost of
 * the tag filter path.
 *
 * `@Disabled` by default. Run manually:
 *
 * ```
 * ./gradlew :server-app:test --tests "io.tolgee.api.v2.controllers.translations.v2TranslationsController.TranslationViewTagFilterPerfTest"
 * ```
 */
@Disabled("Diagnostic perf test — run manually when benchmarking the Translation View query")
@CleanDbBeforeClass
class TranslationViewTagFilterPerfTest : AbstractPartialFixturePerfTestBase() {
  companion object {
    const val TAG_FREQUENCY = 10 // ~1000 keys tagged
    const val TAG_NAME = "tag-common"
  }

  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  override fun setupFilterSpecificFixture() {
    val managedProject = entityManager.find(Project::class.java, projectId)

    val tag =
      Tag().apply {
        name = TAG_NAME
        project = managedProject
      }
    entityManager.persist(tag)
    entityManager.flush()

    val taggedKeyNames = (1..KEY_COUNT).filter { it % TAG_FREQUENCY == 0 }.map { "key %05d".format(it) }
    val taggedKeys =
      entityManager
        .createQuery("FROM Key k WHERE k.project.id = :pid AND k.name IN :names", Key::class.java)
        .setParameter("pid", projectId)
        .setParameter("names", taggedKeyNames)
        .resultList

    for (k in taggedKeys) {
      val meta = KeyMeta(key = k)
      meta.tags.add(tag)
      entityManager.persist(meta)
    }
    entityManager.flush()
    entityManager.clear()
    logger.info("  tagged ${taggedKeys.size} keys with $TAG_NAME")
  }

  @Test
  fun `filterTag=tag-common`() {
    val expected = (KEY_COUNT / TAG_FREQUENCY).toLong()
    val title = "filterTag=$TAG_NAME (~$expected matches)"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = GetTranslationsParams().apply { filterTag = listOf(TAG_NAME) },
          )
        assertThat(result.totalElements).isEqualTo(expected)
      }
    }
  }

  @Test
  fun `filterNoTag=tag-common`() {
    val expected = (KEY_COUNT - KEY_COUNT / TAG_FREQUENCY).toLong()
    val title = "filterNoTag=$TAG_NAME (~$expected matches)"
    measureScenario(title) {
      executeInNewTransaction {
        liftStatementTimeout()
        val result =
          translationViewDataProvider.getData(
            projectId = projectId,
            languages = languageDtos(languages),
            pageable = PageRequest.of(0, PAGE_SIZE),
            params = GetTranslationsParams().apply { filterNoTag = listOf(TAG_NAME) },
          )
        assertThat(result.totalElements).isEqualTo(expected)
      }
    }
  }
}
