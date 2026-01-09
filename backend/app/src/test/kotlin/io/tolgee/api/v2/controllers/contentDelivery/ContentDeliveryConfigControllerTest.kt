package io.tolgee.api.v2.controllers.contentDelivery

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.testing.ContextRecreatingTest
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
    batchJobConcurrentLauncher.pause = false
    tolgeeProperties.contentDelivery.storage.s3.bucketName = null
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
    val mocked = mockS3FileStorage()
    performProjectAuthPost("content-delivery-configs/${testData.defaultServerContentDeliveryConfig.self.id}").andIsOk
    assertStored(mocked)
    assertPruned(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to custom server content delivery config`() {
    tolgeeProperties.contentDelivery.storage.s3.bucketName = "my-bucket"
    val mocked = mockS3FileStorage()
    performProjectAuthPost("content-delivery-configs/${testData.s3ContentDeliveryConfigWithCustomSlug.self.id}").andIsOk
    assertStored(mocked)
    assertPruned(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to azure`() {
    val mocked = mockAzureFileStorage()
    performProjectAuthPost("content-delivery-configs/${testData.azureContentDeliveryConfig.self.id}").andIsOk
    assertStored(mocked)
    assertPruned(mocked)
  }

  private fun assertStored(mocked: FileStorage) {
    mocked.getStoreFileInvocations().assert.hasSize(1)
    getLastStoreFileInvocationPath(mocked)
      .matches("[a-f0-9]{32}/en\\.json".toRegex())
  }

  private fun getLastStoreFileInvocationPath(mocked: FileStorage): String =
    (mocked.getStoreFileInvocations().single().arguments[0] as String)

  private fun assertPruned(mocked: FileStorage) {
    mocked.getPruneDirectoryInvocations().assert.hasSize(1)
    (mocked.getPruneDirectoryInvocations().single().arguments[0] as String)
      .matches("[a-f0-9]{32}".toRegex())
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
