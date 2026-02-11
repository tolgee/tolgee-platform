package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigBranchingTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class ContentDeliveryConfigBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ContentDeliveryConfigBranchingTestData

  @Autowired
  lateinit var contentDeliveryConfigService: ContentDeliveryConfigService

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var branchRepository: BranchRepository

  @Autowired
  private lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @BeforeEach
  fun setup() {
    batchJobConcurrentLauncher.pause = true
    testData = ContentDeliveryConfigBranchingTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled =
      setOf(
        Feature.BRANCHING,
        Feature.MULTIPLE_CONTENT_DELIVERY_CONFIGS,
      )
  }

  @AfterEach
  fun after() {
    batchJobConcurrentLauncher.pause = false
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates CDN config with specific branch`() {
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "New CDN", "filterBranch" to "feature"),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("New CDN")
      node("branchName").isEqualTo("feature")
      node("filterBranch").isEqualTo("feature")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates CDN config without branch defaults to default branch`() {
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "New CDN"),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("New CDN")
      node("branchName").isEqualTo("main")
      node("filterBranch").isEqualTo("main")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates CDN config with non-existent branch returns 404`() {
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "New CDN", "filterBranch" to "non-existent-branch"),
    ).andIsNotFound
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates CDN config to change branch`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.mainBranchCdnConfig.self.id}",
      mapOf("name" to "Main CDN", "filterBranch" to "feature"),
    ).andIsOk.andAssertThatJson {
      node("branchName").isEqualTo("feature")
      node("filterBranch").isEqualTo("feature")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists CDN configs with branchName`() {
    performProjectAuthGet("content-delivery-configs").andIsOk.andAssertThatJson {
      node("_embedded.contentDeliveryConfigs") {
        isArray.hasSize(3)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get single CDN config returns branchName`() {
    performProjectAuthGet("content-delivery-configs/${testData.featureBranchCdnConfig.self.id}")
      .andIsOk
      .andAssertThatJson {
        node("name").isEqualTo("Feature CDN")
        node("branchName").isEqualTo("feature")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `delete branch deletes CDN configs for that branch`() {
    performProjectAuthDelete("branches/${testData.featureBranch.id}").andIsOk

    executeInNewTransaction {
      contentDeliveryConfigService.find(testData.featureBranchCdnConfig.self.id).assert.isNull()
      // main branch configs are unaffected
      contentDeliveryConfigService.find(testData.mainBranchCdnConfig.self.id).assert.isNotNull()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `create CDN config with branch fails when branching not enabled`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.MULTIPLE_CONTENT_DELIVERY_CONFIGS)
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "New CDN", "filterBranch" to "feature"),
    ).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `update CDN config with branch fails when branching not enabled`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.MULTIPLE_CONTENT_DELIVERY_CONFIGS)
    performProjectAuthPut(
      "content-delivery-configs/${testData.mainBranchCdnConfig.self.id}",
      mapOf("name" to "Main CDN", "filterBranch" to "feature"),
    ).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `CDN config filterBranch is derived from branch entity`() {
    executeInNewTransaction {
      val config = contentDeliveryConfigService.get(testData.featureBranchCdnConfig.self.id)
      config.filterBranch.assert.isEqualTo("feature")
      config.branch!!
        .name.assert
        .isEqualTo("feature")
    }
  }
}
