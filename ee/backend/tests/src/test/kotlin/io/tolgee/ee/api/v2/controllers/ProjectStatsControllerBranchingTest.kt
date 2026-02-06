package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.ProjectStatsBranchingTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class ProjectStatsControllerBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ProjectStatsBranchingTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = ProjectStatsBranchingTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `stats without branch use default branch`() {
    performProjectAuthGet("stats")
      .andAssertThatJson {
        node("keyCount").isEqualTo(2)
        node("baseWordsCount").isEqualTo(4)
        node("languageStats").isArray
        node("languageStats[1].languageTag").isEqualTo("de")
        node("languageStats[1].translatedKeyCount").isEqualTo(1)
        node("languageStats[1].translatedWordCount").isEqualTo(2)
        node("languageStats[1].reviewedKeyCount").isEqualTo(1)
        node("languageStats[1].untranslatedKeyCount").isEqualTo(0)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `stats with branch use branch data`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthGet("stats?branch=${testData.featureBranch.name}")
      .andIsOk
      .andAssertThatJson {
        node("keyCount").isEqualTo(1)
        node("baseWordsCount").isEqualTo(3)
        node("languageStats").isArray
        node("languageStats[1].languageTag").isEqualTo("de")
        node("languageStats[1].translatedKeyCount").isEqualTo(1)
        node("languageStats[1].translatedWordCount").isEqualTo(3)
        node("languageStats[1].reviewedKeyCount").isEqualTo(0)
        node("languageStats[1].untranslatedKeyCount").isEqualTo(0)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `stats without branching feature enabled returns forbidden`() {
    enabledFeaturesProvider.forceEnabled = setOf()
    performProjectAuthGet("stats?branch=${testData.featureBranch.name}")
      .andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `stats with unexisting branch returns 404`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthGet("stats?branch=unexisting").andIsNotFound
  }
}
