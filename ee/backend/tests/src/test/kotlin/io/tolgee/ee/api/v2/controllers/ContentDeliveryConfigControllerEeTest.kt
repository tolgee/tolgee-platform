package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.invocation.Invocation
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.ResultActions
import java.util.function.Consumer

class ContentDeliveryConfigControllerEeTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ContentDeliveryConfigTestData

  @Autowired
  lateinit var contentDeliveryConfigService: ContentDeliveryConfigService

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  @MockitoSpyBean
  private lateinit var s3FileStorageFactory: S3FileStorageFactory

  @Autowired
  @MockitoSpyBean
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
    enabledFeaturesProvider.forceEnabled =
      setOf(
        Feature.PROJECT_LEVEL_CONTENT_STORAGES,
        Feature.MULTIPLE_CONTENT_DELIVERY_CONFIGS,
      )
    Mockito.reset(s3FileStorageFactory)
    Mockito.reset(azureFileStorageFactory)
  }

  @AfterEach
  fun after() {
    resetServerProperties()
    batchJobConcurrentLauncher.pause = false
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates content delivery config`() {
    createAzureConfig().andAssertThatJson {
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
  fun `doesnt create when feature not enabled`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.PROJECT_LEVEL_CONTENT_STORAGES)
    createAzureConfig().andIsBadRequest
    enabledFeaturesProvider.forceEnabled =
      setOf(Feature.MULTIPLE_CONTENT_DELIVERY_CONFIGS, Feature.PROJECT_LEVEL_CONTENT_STORAGES)
    createAzureConfig().andIsOk
  }

  private fun createAzureConfig(): ResultActions {
    return performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "Azure 2", "contentStorageId" to testData.azureContentStorage.self.id),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates content delivery config with auto publish`() {
    val mock = mockAzureFileStorage()
    var id: Long? = null
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "Azure 2", "contentStorageId" to testData.azureContentStorage.self.id, "autoPublish" to true),
    ).andAssertThatJson {
      node("id").isNumber.satisfies(
        Consumer {
          id = it.toLong()
        },
      )
      node("autoPublish").isEqualTo(true)
    }

    executeInNewTransaction {
      contentDeliveryConfigService
        .get(id!!)
        .automationActions.assert.isNotEmpty
    }

    assertPruned(mock)
    assertStored(mock)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates content delivery config without pruning`() {
    val mock = mockAzureFileStorage()
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf(
        "name" to "Azure 2",
        "contentStorageId" to testData.azureContentStorage.self.id,
        "autoPublish" to true,
        "pruneBeforePublish" to false,
      ),
    )

    assertStored(mock)
    assertNotPruned(mock)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `removes the automation on update`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}",
      mapOf("name" to "DS", "autoPublish" to false),
    )
    executeInNewTransaction {
      contentDeliveryConfigService
        .get(testData.defaultServerContentDeliveryConfig.self.id)
        .automationActions.assert
        .isEmpty()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates content delivery config`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.s3ContentDeliveryConfig.self.id}",
      mapOf("name" to "S3 2", "contentStorageId" to testData.s3ContentStorage.self.id),
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
        isArray.hasSize(4)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get single`() {
    performProjectAuthGet("content-delivery-configs/${testData.s3ContentDeliveryConfig.self.id}")
      .andIsOk
      .andAssertThatJson {
        node("name").isEqualTo("S3")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to s3`() {
    val mocked = mockS3FileStorage()
    performProjectAuthPost("content-delivery-configs/${testData.s3ContentDeliveryConfig.self.id}")
    assertStored(mocked)
    assertPruned(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates with custom slug`() {
    createWithCustomSlug().andAssertThatJson {
      node("slug").isEqualTo("my-slug")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not create with custom slug without custom storage`() {
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf(
        "name" to "Azure 2",
        "slug" to "my-slug",
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.CUSTOM_SLUG_IS_ONLY_APPLICABLE_FOR_CUSTOM_STORAGE)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `adds custom slug on update`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.s3ContentDeliveryConfig.self.id}",
      mapOf("name" to "S3 2", "contentStorageId" to testData.s3ContentStorage.self.id, "slug" to "my-slug"),
    ).andAssertThatJson {
      node("slug").isEqualTo("my-slug")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not set custom slug on update without custom storage`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.s3ContentDeliveryConfig.self.id}",
      mapOf("name" to "No no!", "slug" to "my-slug"),
    ).andIsBadRequest.andHasErrorMessage(Message.CUSTOM_SLUG_IS_ONLY_APPLICABLE_FOR_CUSTOM_STORAGE)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `regenerates slug on update`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.s3ContentDeliveryConfigWithCustomSlug.self.id}",
      mapOf("name" to "S3", "contentStorageId" to testData.s3ContentStorage.self.id),
    ).andAssertThatJson { node("slug").isString.matches("[a-f0-9]{32}") }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `throws when custom storage removed and generated slug is kept`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.s3ContentDeliveryConfigWithCustomSlug.self.id}",
      mapOf("name" to "S3", "slug" to "my-slug"),
    ).andIsBadRequest.andHasErrorMessage(Message.CUSTOM_SLUG_IS_ONLY_APPLICABLE_FOR_CUSTOM_STORAGE)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not regenerate slug when custom storage not removed`() {
    val slug = testData.defaultServerContentDeliveryConfig.self.slug
    performProjectAuthPut(
      "content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}",
      mapOf("name" to "S3 new", "slug" to slug),
    ).andIsOk.andAssertThatJson {
      node("slug").isEqualTo(slug)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not regenerate slug when not using custom storage`() {
    val slug = testData.defaultServerContentDeliveryConfig.self.slug
    performProjectAuthPut(
      "content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}",
      mapOf("name" to "S3 new"),
    ).andIsOk.andAssertThatJson {
      node("slug").isEqualTo(slug)
    }
  }

  private fun createWithCustomSlug() =
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf(
        "name" to "Azure 2",
        "contentStorageId" to testData.azureContentStorage.self.id,
        "slug" to "my-slug",
      ),
    )

  private fun assertStored(mocked: FileStorage) {
    mocked.getStoreFileInvocations().assert.hasSize(1)
    (mocked.getStoreFileInvocations().single().arguments[0] as String)
      .matches("[a-f0-9]{32}/en\\.json".toRegex())
  }

  private fun assertPruned(mocked: FileStorage) {
    mocked.getPruneDirectoryInvocations().assert.hasSize(1)
    (mocked.getPruneDirectoryInvocations().single().arguments[0] as String)
      .matches("[a-f0-9]{32}".toRegex())
  }

  private fun assertNotPruned(mocked: FileStorage) {
    mocked.getPruneDirectoryInvocations().assert.hasSize(0)
  }

  private fun FileStorage.getInvocations(): List<Invocation> = Mockito.mockingDetails(this).invocations.toList()

  private fun FileStorage.getStoreFileInvocations(): List<Invocation> {
    return getInvocations().filter { it.method.name == "storeFile" }
  }

  private fun FileStorage.getPruneDirectoryInvocations(): List<Invocation> {
    return getInvocations().filter { it.method.name == "pruneDirectory" }
  }

  private fun resetServerProperties() {
    tolgeeProperties.contentDelivery.storage.s3
      .clear()
    tolgeeProperties.contentDelivery.storage.azure
      .clear()
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
