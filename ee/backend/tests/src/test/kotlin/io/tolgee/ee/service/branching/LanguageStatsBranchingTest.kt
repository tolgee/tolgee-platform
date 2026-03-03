package io.tolgee.ee.service.branching

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BranchTranslationsTestData
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.queryResults.LanguageStatsDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.security.ProjectHolder
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class LanguageStatsBranchingTest : AbstractSpringTest() {
  @Autowired
  private lateinit var projectHolder: ProjectHolder

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: BranchTranslationsTestData

  @BeforeEach
  fun setup() {
    testData = BranchTranslationsTestData()
    testDataService.saveTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
  }

  @AfterEach
  fun tearDown() {
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `updates stats when added key on branch`() {
    val stats = getProjectStats("de")!!
    val branchStats = getProjectStats("de", testData.protectedBranch.id)!!

    createKey("das new key", mapOf("en" to "hello my friend"), testData.protectedBranch.name)

    waitForNotThrowing(AssertionFailedError::class) {
      val newBranchStats = getProjectStats("de", testData.protectedBranch.id)!!
      // the branch stats are updated
      assertThat(newBranchStats.untranslatedWords).isEqualTo(branchStats.untranslatedWords.plus(3))
      assertThat(newBranchStats.untranslatedKeys).isEqualTo(branchStats.untranslatedKeys.plus(1))
      assertThat(newBranchStats.translationsUpdatedAt!!.after(branchStats.translationsUpdatedAt))

      val newStats = getProjectStats("de")!!
      // the default branch stats are not changed
      assertThat(newStats.untranslatedWords).isEqualTo(stats.untranslatedWords)
      assertThat(newStats.untranslatedKeys).isEqualTo(stats.untranslatedKeys)
      assertThat(newStats.translationsUpdatedAt).isEqualTo(stats.translationsUpdatedAt)
    }
  }

  private fun getProjectStats(
    languageTag: String,
    branchId: Long? = null,
  ): LanguageStatsDto? {
    val projectLanguages = languageService.getProjectLanguages(testData.project.id).associateBy { it.id }
    return executeInNewTransaction(platformTransactionManager) {
      languageStatsService
        .getLanguageStats(projectId = testData.project.id, projectLanguages.keys, branchId)
        .find { projectLanguages[it.languageId]!!.tag == languageTag }
    }
  }

  private fun createKey(
    name: String,
    translations: Map<String, String>,
    branch: String? = null,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      val projectDto = ProjectDto.fromEntity(testData.project)
      projectHolder.project = projectDto
      keyService.create(
        testData.project,
        CreateKeyDto(name = name, translations = translations, branch = branch),
      )
    }
  }
}
