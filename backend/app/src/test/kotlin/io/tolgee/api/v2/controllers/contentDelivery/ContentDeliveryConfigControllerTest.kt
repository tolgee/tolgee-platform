package io.tolgee.api.v2.controllers.contentDelivery

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean

@ContextRecreatingTest
class ContentDeliveryConfigControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: ContentDeliveryConfigTestData

  @Autowired
  lateinit var contentDeliveryConfigService: ContentDeliveryConfigService

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  @SpyBean
  private lateinit var s3FileStorageFactory: S3FileStorageFactory

  @Autowired
  @SpyBean
  private lateinit var azureFileStorageFactory: AzureFileStorageFactory

  @Autowired
  private lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @BeforeEach
  fun setup() {
    batchJobConcurrentLauncher.pause = true
    testData = ContentDeliveryConfigTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = listOf(Feature.PROJECT_LEVEL_CONTENT_STORAGES)
    Mockito.reset(s3FileStorageFactory)
    Mockito.reset(azureFileStorageFactory)
  }

  @AfterEach
  fun after() {
    resetServerProperties()
    batchJobConcurrentLauncher.pause = false
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates content delivery config`() {
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "Azure 2", "contentStorageId" to testData.azureContentStorage.self.id)
    ).andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo("Azure 2")
      node("slug").isString.hasSize(32)
      node("format").isEqualTo("JSON")
      node("autoPublish").isEqualTo(false)
      node("storage") {
        node("name").isEqualTo("Azure")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates content delivery config with auto publish`() {
    val mock = mockAzureFileStorage()
    var id: Long? = null
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "Azure 2", "contentStorageId" to testData.azureContentStorage.self.id, "autoPublish" to true)
    ).andAssertThatJson {
      node("id").isNumber.satisfies {
        id = it.toLong()
      }
      node("autoPublish").isEqualTo(true)
    }

    executeInNewTransaction {
      contentDeliveryConfigService.get(id!!).automationActions.assert.isNotEmpty
    }

    assertStored(mock)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `removes the automation on update`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}",
      mapOf("name" to "DS", "autoPublish" to false)
    )
    executeInNewTransaction {
      contentDeliveryConfigService
        .get(testData.defaultServerContentDeliveryConfig.self.id)
        .automationActions.assert.isEmpty()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates content delivery config`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.s3ContentDeliveryConfig.self.id}",
      mapOf("name" to "S3 2", "contentStorageId" to testData.s3ContentStorage.self.id)
    ).andAssertThatJson {
      node("name").isEqualTo("S3 2")
      node("storage") {
        node("name").isEqualTo("S3")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists content delivery configs`() {
    performProjectAuthGet("content-delivery-configs").andIsOk.andAssertThatJson {
      node("_embedded.contentDeliveryConfigs") {
        isArray.hasSize(3)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get single`() {
    performProjectAuthGet("content-delivery-configs/${testData.s3ContentDeliveryConfig.self.id}").andIsOk
      .andAssertThatJson {
        node("name").isEqualTo("S3")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes content delivery config`() {
    performProjectAuthDelete(
      "content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}"
    ).andIsOk
    contentDeliveryConfigService.find(testData.defaultServerContentDeliveryConfig.self.id).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to s3`() {
    val mocked = mockS3FileStorage()
    performProjectAuthPost("content-delivery-configs/${testData.s3ContentDeliveryConfig.self.id}")
    assertStored(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to azure`() {
    val mocked = mockAzureFileStorage()
    performProjectAuthPost("content-delivery-configs/${testData.azureContentDeliveryConfig.self.id}").andIsOk
    assertStored(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to default server content delivery config`() {
    tolgeeProperties.contentDelivery.storage.s3.bucketName = "my-bucket"
    val mocked = mockS3FileStorage()
    performProjectAuthPost("content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}").andIsOk
    assertStored(mocked)
  }

  private fun assertStored(mocked: FileStorage) {
    Mockito.mockingDetails(mocked).invocations.assert.hasSize(1)
    (Mockito.mockingDetails(mocked).invocations.single().arguments[0] as String)
      .matches("[a-f0-9]{32}/en\\.json".toRegex())
  }

  private fun resetServerProperties() {
    tolgeeProperties.contentDelivery.storage.s3.clear()
    tolgeeProperties.contentDelivery.storage.azure.clear()
  }

  private fun mockS3FileStorage(): S3FileStorage {
    val mockedFileStorage = mock<S3FileStorage>()
    doAnswer {
      mockedFileStorage
    }.whenever(s3FileStorageFactory).create(any())
    return mockedFileStorage
  }

  private fun mockAzureFileStorage(): AzureBlobFileStorage {
    val mockedFileStorage = mock<AzureBlobFileStorage>()
    doAnswer {
      mockedFileStorage
    }.whenever(azureFileStorageFactory).create(any())
    return mockedFileStorage
  }
}
