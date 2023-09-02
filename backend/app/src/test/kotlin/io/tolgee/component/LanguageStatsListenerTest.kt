package io.tolgee.component

import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.opentest4j.AssertionFailedError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean

class LanguageStatsListenerTest : AbstractControllerTest() {

  @Autowired
  @SpyBean
  private lateinit var authenticationFacade: AuthenticationFacade

  @Autowired
  private lateinit var projectHolder: ProjectHolder

  @Test
  fun `updates stats when added key`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val deutschStats =
      executeInNewTransaction(platformTransactionManager) {
        languageStatsService.getLanguageStats(projectId = testData.project.id)
          .find { it.language.tag == "de" }
      }

    executeInNewTransaction(platformTransactionManager) {
      val projectDto = ProjectDto.fromEntity(testData.project)
      Mockito.`when`(authenticationFacade.targetProjectOrNull).thenReturn(projectDto)
      projectHolder.project = projectDto

      keyService.create(
        testData.project,
        CreateKeyDto(
          name = "ho ho ho new key",
          translations = mapOf("en" to "hello")
        )
      )
    }

    // it's async so lets wait until it passes
    waitForNotThrowing(AssertionFailedError::class) {
      executeInNewTransaction(platformTransactionManager) {
        val newDeutschStats = languageStatsService.getLanguageStats(projectId = testData.project.id)
          .find { it.language.tag == "de" }
        assertThat(newDeutschStats!!.untranslatedWords - 1).isEqualTo(deutschStats?.untranslatedWords)
      }
    }
  }
}
