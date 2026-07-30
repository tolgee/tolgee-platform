package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.Date

class ProjectContributorsApiKeyTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ContributorsTestData

  @BeforeEach
  fun setup() {
    testData = ContributorsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    projectSupplier = { testData.project }
    recordProjectActivity(testData.project.id, testData.contributor.id, Date(1_600_000_000_000))
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.MEMBERS_VIEW])
  fun `rejects an API key even when it holds members-view, because super authentication cannot be met`() {
    performProjectAuthGet("${testData.project.id}/contributors").andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW])
  fun `rejects an API key whose scopes exclude members-view`() {
    performProjectAuthGet("${testData.project.id}/contributors").andIsForbidden
  }
}
