package io.tolgee.component

import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.security.ProjectHolder
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import org.springframework.beans.factory.annotation.Autowired

class LanguageStatsListenerTest : AbstractControllerTest() {
  @Autowired
  private lateinit var projectHolder: ProjectHolder

  @Test
  fun `updates stats when added key`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val projectLanguages = languageService.getProjectLanguages(testData.project.id).associateBy { it.id }

    val deutschStats =
      executeInNewTransaction(platformTransactionManager) {
        languageStatsService
          .getLanguageStats(projectId = testData.project.id)
          .find { projectLanguages[it.languageId]!!.tag == "de" }
      }

    executeInNewTransaction(platformTransactionManager) {
      val projectDto = ProjectDto.fromEntity(testData.project)
      projectHolder.project = projectDto

      keyService.create(
        testData.project,
        CreateKeyDto(
          name = "ho ho ho new key",
          translations = mapOf("en" to "hello"),
        ),
      )
    }

    // it's async so lets wait until it passes
    waitForNotThrowing(AssertionFailedError::class) {
      executeInNewTransaction(platformTransactionManager) {
        val newDeutschStats =
          languageStatsService
            .getLanguageStats(projectId = testData.project.id)
            .find { projectLanguages[it.languageId]!!.tag == "de" }
        assertThat(newDeutschStats!!.untranslatedWords - 1).isEqualTo(deutschStats?.untranslatedWords)
        assertThat(newDeutschStats.translationsUpdatedAt!!.after(deutschStats!!.translationsUpdatedAt))
      }
    }
  }
}
