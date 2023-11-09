package io.tolgee.api.v2.controllers.cdn

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.CdnExporterTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.service.cdn.CdnExporterService
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

class CdnExporterControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: CdnExporterTestData

  @Autowired
  lateinit var cdnExporterService: CdnExporterService

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  @SpyBean
  private lateinit var s3FileStorageFactory: S3FileStorageFactory

  @Autowired
  @SpyBean
  private lateinit var azureFileStorageFactory: AzureFileStorageFactory

  @BeforeEach
  fun setup() {
    testData = CdnExporterTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = listOf(Feature.PROJECT_LEVEL_CDN_STORAGES)
  }

  @AfterEach
  fun after() {
    resetServerProperties()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates exporter`() {
    performProjectAuthPost(
      "cdn-exporters",
      mapOf("name" to "Azure 2", "cdnStorageId" to testData.azureCdnStorage.self.id)
    ).andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo("Azure 2")
      node("format").isEqualTo("JSON")
      node("autoPublish").isEqualTo(false)
      node("storage") {
        node("name").isEqualTo("Azure")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates exporter with auto publish`() {
    var id: Long? = null
    performProjectAuthPost(
      "cdn-exporters",
      mapOf("name" to "Azure 2", "cdnStorageId" to testData.azureCdnStorage.self.id, "autoPublish" to true)
    ).andAssertThatJson {
      node("id").isNumber.satisfies {
        id = it.toLong()
      }
      node("autoPublish").isEqualTo(true)
    }

    executeInNewTransaction {
      cdnExporterService.get(id!!).automationActions.assert.isNotEmpty
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `removes the automation on update`() {
    performProjectAuthPut(
      "cdn-exporters/${testData.defaultServerExporter.self.id}",
      mapOf("name" to "DS", "autoPublish" to false)
    )
    executeInNewTransaction {
      cdnExporterService.get(testData.defaultServerExporter.self.id).automationActions.assert.isEmpty()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates exporter`() {
    performProjectAuthPut(
      "cdn-exporters/${testData.s3Exporter.self.id}",
      mapOf("name" to "S3 2", "cdnStorageId" to testData.s3CdnStorage.self.id)
    ).andAssertThatJson {
      node("name").isEqualTo("S3 2")
      node("storage") {
        node("name").isEqualTo("S3")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists exporters`() {
    performProjectAuthGet("cdn-exporters").andIsOk.andAssertThatJson {
      node("_embedded.exporters") {
        isArray.hasSize(3)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get single`() {
    performProjectAuthGet("cdn-exporters/${testData.s3Exporter.self.id}").andIsOk
      .andAssertThatJson {
        node("name").isEqualTo("S3")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes exporter`() {
    performProjectAuthDelete(
      "cdn-exporters/${testData.s3Exporter.self.id}"
    ).andIsOk
    cdnExporterService.find(testData.s3Exporter.self.id).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to s3`() {
    val mocked = mockS3FileStorage()
    performProjectAuthPost("cdn-exporters/${testData.s3Exporter.self.id}")
    assertStores(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to azure`() {
    val mocked = mockAzureFileStorage()
    performProjectAuthPost("cdn-exporters/${testData.azureExporter.self.id}").andIsOk
    assertStores(mocked)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishes to default server exporter`() {
    tolgeeProperties.cdn.s3.bucketName = "my-bucket"
    val mocked = mockS3FileStorage()
    performProjectAuthPost("cdn-exporters/${testData.defaultServerExporter.self.id}").andIsOk
    assertStores(mocked)
  }

  private fun assertStores(mocked: FileStorage) {
    Mockito.mockingDetails(mocked).invocations.assert.hasSize(1)
    (Mockito.mockingDetails(mocked).invocations.single().arguments[0] as String).contains("/test-project/en.json")
  }

  private fun resetServerProperties() {
    tolgeeProperties.cdn.s3.clear()
    tolgeeProperties.cdn.azure.clear()
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
