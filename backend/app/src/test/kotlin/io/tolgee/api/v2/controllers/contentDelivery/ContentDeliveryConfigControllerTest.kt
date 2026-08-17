package io.tolgee.api.v2.controllers.contentDelivery

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.fixtures.CONTENT_DELIVERY_GENERATED_SLUG_PATTERN
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.assertPrunedSingleDirectory
import io.tolgee.fixtures.assertStoredSingleFile
import io.tolgee.fixtures.mockCreatedStorage
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@ContextRecreatingTest
class ContentDeliveryConfigControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ContentDeliveryConfigTestData

  @Autowired
  lateinit var contentDeliveryConfigService: ContentDeliveryConfigService

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
    Mockito.reset(s3FileStorageFactory)
    Mockito.reset(azureFileStorageFactory)
  }

  @AfterEach
  fun after() {
    resetServerProperties()
    testDataService.cleanTestData(testData.root)
    batchJobConcurrentLauncher.pause = false
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `throws when custom slug is used with default storage`() {
    performProjectAuthPut(
      "content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}",
      mapOf("name" to "S3 new", "slug" to "hello"),
    ).andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes content delivery config`() {
    performProjectAuthDelete(
      "content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}",
    ).andIsOk
    contentDeliveryConfigService.find(testData.defaultServerContentDeliveryConfig.self.id).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to default server content delivery config`() {
    tolgeeProperties.contentDelivery.storage.s3.bucketName = "my-bucket"
    val mocked = s3FileStorageFactory.mockCreatedStorage()
    performProjectAuthPost("content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}").andIsOk
    assertStored(mocked)
    assertPruned(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to custom server content delivery config`() {
    tolgeeProperties.contentDelivery.storage.s3.bucketName = "my-bucket"
    val mocked = s3FileStorageFactory.mockCreatedStorage()
    performProjectAuthPost("content-delivery-configs/${testData.s3ContentDeliveryConfigWithCustomSlug.self.id}").andIsOk
    assertStored(mocked, slugPattern = "my-slug")
    assertPruned(mocked, slugPattern = "my-slug")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to azure`() {
    val mocked = azureFileStorageFactory.mockCreatedStorage()
    performProjectAuthPost("content-delivery-configs/${testData.azureContentDeliveryConfig.self.id}").andIsOk
    assertStored(mocked)
    assertPruned(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes as zip when zip option is enabled`() {
    tolgeeProperties.contentDelivery.storage.s3.bucketName = "my-bucket"
    val mocked = s3FileStorageFactory.mockCreatedStorage()
    performProjectAuthPost("content-delivery-configs/${testData.zipEnabledContentDeliveryConfig.self.id}").andIsOk
    assertStoredAsZip(mocked)
    assertPruned(mocked)
  }

  private fun assertStored(
    mocked: FileStorage,
    slugPattern: String = CONTENT_DELIVERY_GENERATED_SLUG_PATTERN,
  ) = mocked.assertStoredSingleFile("$slugPattern/en\\.json", "application/json")

  private fun assertStoredAsZip(mocked: FileStorage) =
    mocked.assertStoredSingleFile("$CONTENT_DELIVERY_GENERATED_SLUG_PATTERN/translations\\.zip", "application/zip")

  private fun assertPruned(
    mocked: FileStorage,
    slugPattern: String = CONTENT_DELIVERY_GENERATED_SLUG_PATTERN,
  ) = mocked.assertPrunedSingleDirectory(slugPattern)

  private fun resetServerProperties() {
    tolgeeProperties.contentDelivery.storage.s3
      .clear()
    tolgeeProperties.contentDelivery.storage.azure
      .clear()
  }
}
