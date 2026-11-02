package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BranchTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.repository.BatchJobRepository
import io.tolgee.service.branching.BranchService
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BatchJobsProtectedBranchTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: BranchTestData
  lateinit var mainKey: Key
  lateinit var featureKey: Key

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var branchService: BranchService

  @Autowired
  lateinit var batchJobRepository: BatchJobRepository

  @BeforeEach
  fun setup() {
    testData = BranchTestData(currentDateProvider)
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    val project = testData.projectBuilder.self
    branchService.setProtected(project.id, testData.mainBranch.id, true)
    mainKey = keyService.create(project, CreateKeyDto(name = "main-key", branch = testData.mainBranch.name))
    featureKey = keyService.create(project, CreateKeyDto(name = "feature-key", branch = testData.featureBranch.name))
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE])
  fun `batch delete of a protected branch key is forbidden without protected-modify`() {
    performProjectAuthPost("start-batch-job/delete-keys", mapOf("keyIds" to listOf(mainKey.id))).andIsForbidden
    batchJobRepository.findAll().assert.isEmpty()
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE, Scope.BRANCH_PROTECTED_MODIFY])
  fun `batch delete of a protected branch key passes with protected-modify`() {
    performProjectAuthPost("start-batch-job/delete-keys", mapOf("keyIds" to listOf(mainKey.id))).andIsOk
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE])
  fun `batch delete on an unprotected branch needs no extra scope`() {
    performProjectAuthPost("start-batch-job/delete-keys", mapOf("keyIds" to listOf(featureKey.id))).andIsOk
  }
}
