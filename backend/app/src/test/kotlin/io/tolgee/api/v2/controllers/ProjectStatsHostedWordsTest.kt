package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ProjectStatsHostedWordsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectStatsHostedWordsTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ProjectStatsHostedWordsTestData

  @BeforeEach
  fun setup() {
    testData = ProjectStatsHostedWordsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `counts each translation's own words`() {
    performProjectAuthGet("stats").andIsOk.andAssertThatJson {
      node("hostedWordsCount").isEqualTo(6)
      node("baseWordsCount").isEqualTo(5)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `refreshes stats saved before hosted words existed`() {
    executeInNewTransaction {
      entityManager.createNativeQuery("update language_stats set hosted_words = null").executeUpdate()
    }
    performProjectAuthGet("stats").andIsOk.andAssertThatJson {
      node("hostedWordsCount").isEqualTo(6)
    }
  }
}
