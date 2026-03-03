package io.tolgee.component

import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.queryResults.LanguageStatsDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.security.ProjectHolder
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class LanguageStatsListenerTest : AbstractControllerTest() {
  @Autowired
  private lateinit var projectHolder: ProjectHolder

  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `updates stats when added key`() {
    val stats = getProjectStats("de")

    createKey("ho ho ho new key", mapOf("en" to "hello"))

    // it's async, so lets wait until it passes
    waitForNotThrowing(AssertionFailedError::class) {
      val newStats = getProjectStats("de")
      assertThat(newStats!!.untranslatedWords).isEqualTo(stats?.untranslatedWords?.plus(1))
      assertThat(newStats.translationsUpdatedAt!!.after(stats!!.translationsUpdatedAt))
    }
  }

  private fun getProjectStats(languageTag: String): LanguageStatsDto? {
    val projectLanguages = languageService.getProjectLanguages(testData.project.id).associateBy { it.id }
    return executeInNewTransaction(platformTransactionManager) {
      languageStatsService
        .getLanguageStats(projectId = testData.project.id, projectLanguages.keys, null)
        .find { projectLanguages[it.languageId]!!.tag == languageTag }
    }
  }

  private fun createKey(
    name: String,
    translations: Map<String, String>,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      val projectDto = ProjectDto.fromEntity(testData.project)
      projectHolder.project = projectDto
      keyService.create(
        testData.project,
        CreateKeyDto(name = name, translations = translations),
      )
    }
  }
}
