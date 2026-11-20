package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class ContentStorageUrlSecurityTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ContentDeliveryConfigTestData

  @MockitoSpyBean
  @Autowired
  private lateinit var contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = ContentDeliveryConfigTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.PROJECT_LEVEL_CONTENT_STORAGES)
    internalProperties.disableUrlSsrfProtection = false
    tolgeeProperties.contentDelivery.storage.allowLocalAddresses = false
    Mockito.reset(contentDeliveryFileStorageProvider)
    doAnswer { mock<S3FileStorage>() }.whenever(contentDeliveryFileStorageProvider).getStorage(any(), any())
  }

  @AfterEach
  fun after() {
    testDataService.cleanTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = null
    internalProperties.disableUrlSsrfProtection = true
    tolgeeProperties.contentDelivery.storage.allowLocalAddresses = false
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rejects internal s3 endpoint on test`() {
    performProjectAuthPost("content-storages/test", s3Request("http://169.254.169.254/latest/meta-data/"))
      .andIsOk
      .andAssertThatJson {
        node("success").isBoolean.isFalse
        node("message").isEqualTo(Message.URL_NOT_VALID.code)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rejects internal s3 endpoint on create`() {
    performProjectAuthPost("content-storages", s3Request("http://127.0.0.1:9000"))
      .andIsBadRequest
      .andHasErrorMessage(Message.CONTENT_STORAGE_CONFIG_INVALID)
      .andAssertThatJson { node("params[0]").isEqualTo(Message.URL_NOT_VALID.code) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rejects internal s3 endpoint on update`() {
    val id = testData.s3ContentStorage.self.id
    performProjectAuthPut("content-storages/$id", s3Request("http://10.0.0.1"))
      .andIsBadRequest
      .andHasErrorMessage(Message.CONTENT_STORAGE_CONFIG_INVALID)
      .andAssertThatJson { node("params[0]").isEqualTo(Message.URL_NOT_VALID.code) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rejects scheme-less internal s3 endpoint`() {
    performProjectAuthPost("content-storages/test", s3Request("localhost:9000"))
      .andIsOk
      .andAssertThatJson { node("message").isEqualTo(Message.URL_NOT_VALID.code) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rejects internal azure blob endpoint on test`() {
    val connectionString =
      "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=a2V5;" +
        "BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
    performProjectAuthPost("content-storages/test", azureRequest(connectionString))
      .andIsOk
      .andAssertThatJson { node("message").isEqualTo(Message.URL_NOT_VALID.code) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `allows internal s3 endpoint when allowLocalAddresses is true`() {
    tolgeeProperties.contentDelivery.storage.allowLocalAddresses = true
    performProjectAuthPost("content-storages/test", s3Request("http://127.0.0.1:9000"))
      .andIsOk
      .andAssertThatJson { node("success").isBoolean.isTrue }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `allows public s3 endpoint`() {
    performProjectAuthPost("content-storages/test", s3Request("https://93.184.216.34"))
      .andIsOk
      .andAssertThatJson { node("success").isBoolean.isTrue }
  }

  private fun s3Request(endpoint: String) =
    mapOf(
      "name" to "s3",
      "s3ContentStorageConfig" to
        mapOf(
          "bucketName" to "bucket",
          "accessKey" to "accessKey",
          "secretKey" to "secretKey",
          "endpoint" to endpoint,
          "signingRegion" to "eu-central-1",
        ),
    )

  private fun azureRequest(connectionString: String) =
    mapOf(
      "name" to "azure",
      "azureContentStorageConfig" to
        mapOf(
          "connectionString" to connectionString,
          "containerName" to "container",
        ),
    )
}
