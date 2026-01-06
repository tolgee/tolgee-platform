package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.service.ContentStorageService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.contentDelivery.ContentStorage
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
import org.springframework.data.domain.Pageable
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.ResultActions
import java.math.BigDecimal
import java.util.function.Consumer

class ContentStorageControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ContentDeliveryConfigTestData

  @Autowired
  private lateinit var contentStorageService: ContentStorageService

  @MockitoSpyBean
  @Autowired
  private lateinit var contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun beforeEach() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.PROJECT_LEVEL_CONTENT_STORAGES)
    testData = ContentDeliveryConfigTestData()
    Mockito.reset(contentDeliveryFileStorageProvider)
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  @AfterEach
  fun clenup() {
    enabledFeaturesProvider.forceEnabled = setOf()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates Content Storage`() {
    performCreate()
    executeInNewTransaction {
      val all = contentStorageService.getAllInProject(project.id, Pageable.ofSize(100)).sortedBy { it.id }
      all.assert.hasSize(3)
      val azureContentStorageConfig = all.last().azureContentStorageConfig!!
      azureContentStorageConfig.connectionString.assert.isEqualTo("fakeConnectionString")
      azureContentStorageConfig.containerName.assert.isEqualTo("fakeContainerName")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates Content Storage with custom path`() {
    doAnswer {
      mock<S3FileStorage>()
    }.whenever(contentDeliveryFileStorageProvider).getStorage(any())

    performProjectAuthPost(
      "content-storages",
      mapOf(
        "name" to "s3",
        "s3ContentStorageConfig" to
          mapOf(
            "bucketName" to "bucketName",
            "accessKey" to "accessKey",
            "secretKey" to "secretKey",
            "endpoint" to "endpoint",
            "signingRegion" to "signingRegion",
            "path" to "custom/path",
          ),
      ),
    ).andIsOk
    val all =
      contentStorageService
        .getAllInProject(project.id, Pageable.ofSize(100))
        .sortedBy { it.id }
    all
      .last()
      .s3ContentStorageConfig!!
      .path.assert
      .isEqualTo("custom/path")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists storages`() {
    performCreate()
    performProjectAuthGet("content-storages").andAssertThatJson {
      node("_embedded.contentStorages").isArray.hasSize(3)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns single storage`() {
    val (storage) = performCreate()
    performProjectAuthGet("content-storages/${storage.id}").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Azure")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates Content Storage`() {
    val (storage) = performCreate()
    performProjectAuthPut(
      "content-storages/${storage.id}",
      mapOf(
        "name" to "S3",
        "s3ContentStorageConfig" to
          mapOf(
            "bucketName" to "new bucketName",
            "accessKey" to "new accessKey",
            "secretKey" to "new secretKey",
            "endpoint" to "new endpoint",
            "signingRegion" to "new signingRegion",
          ),
      ),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("S3")
    }

    executeInNewTransaction {
      val updatedStorage = contentStorageService.get(storage.id)
      updatedStorage.s3ContentStorageConfig!!
        .bucketName.assert
        .isEqualTo("new bucketName")
      updatedStorage.s3ContentStorageConfig!!
        .accessKey.assert
        .isEqualTo("new accessKey")
      updatedStorage.s3ContentStorageConfig!!
        .secretKey.assert
        .isEqualTo("new secretKey")
      updatedStorage.s3ContentStorageConfig!!
        .endpoint.assert
        .isEqualTo("new endpoint")
      updatedStorage.s3ContentStorageConfig!!
        .signingRegion.assert
        .isEqualTo("new signingRegion")
      updatedStorage.azureContentStorageConfig.assert.isNull()
    }

    // test it keeps the old secrets when empty
    performProjectAuthPut(
      "content-storages/${storage.id}",
      mapOf(
        "name" to "S3",
        "s3ContentStorageConfig" to
          mapOf(
            "bucketName" to "new bucketName",
            "endpoint" to "new endpoint",
            "signingRegion" to "new signingRegion",
          ),
      ),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("S3")
    }

    executeInNewTransaction {
      val updatedStorage = contentStorageService.get(storage.id)
      updatedStorage.s3ContentStorageConfig!!
        .accessKey.assert
        .isEqualTo("new accessKey")
      updatedStorage.s3ContentStorageConfig!!
        .secretKey.assert
        .isEqualTo("new secretKey")
      updatedStorage.azureContentStorageConfig.assert.isNull()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates Content Storage to the same type`() {
    val (storage) = performCreate()
    performProjectAuthPut(
      "content-storages/${storage.id}",
      mapOf(
        "name" to "Azure",
        "azureContentStorageConfig" to
          mapOf(
            "connectionString" to "fakeConnectionString",
            "containerName" to "fakeContainerName",
          ),
      ),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("Azure")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `validates Azure Config storage`() {
    performProjectAuthPost(
      "content-storages",
      mapOf(
        "azureContentStorageConfig" to
          mapOf(
            "connectionString" to "fakeConnectionString",
            "containerName" to "fakeContainerName",
          ),
      ),
    ).andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes an storage`() {
    val (storage) = performCreate()
    performProjectAuthDelete(
      "content-storages/${storage.id}",
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes not delete when in use`() {
    performProjectAuthDelete(
      "content-storages/${testData.azureContentStorage.self.id}",
    ).andIsBadRequest.andHasErrorMessage(Message.CONTENT_STORAGE_IS_IN_USE)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tests azure storage`() {
    performProjectAuthPost(
      "content-storages/test",
      mapOf(
        "name" to "azure",
        "azureContentStorageConfig" to
          mapOf(
            "connectionString" to "fakeConnectionString",
            "containerName" to "fakeContainerName",
          ),
      ),
    ).andAssertThatJson {
      node("success").isBoolean.isFalse
    }.andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tests new configuration for existing azure storage`() {
    val (storage) = performCreate()
    performProjectAuthPost(
      "content-storages/${storage.id}/test",
      mapOf(
        "name" to "azure",
        "azureContentStorageConfig" to
          mapOf(
            "containerName" to "fakeContainerName",
          ),
      ),
    ).andAssertThatJson {
      node("success").isBoolean.isTrue
    }.andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tests s3 storage`() {
    performProjectAuthPost(
      "content-storages/test",
      mapOf(
        "name" to "s3",
        "s3ContentStorageConfig" to
          mapOf(
            "bucketName" to "bucketName",
            "accessKey" to "accessKey",
            "secretKey" to "secretKey",
            "endpoint" to "endpoint",
            "signingRegion" to "signingRegion",
          ),
      ),
    ).andIsOk
  }

  private fun performCreate(): Pair<ContentStorage, ResultActions> {
    doAnswer {
      mock<AzureBlobFileStorage>()
    }.whenever(contentDeliveryFileStorageProvider).getStorage(any())

    var id: Long? = null

    val result =
      performProjectAuthPost(
        "content-storages",
        mapOf(
          "name" to "Azure",
          "azureContentStorageConfig" to
            mapOf(
              "connectionString" to "fakeConnectionString",
              "containerName" to "fakeContainerName",
            ),
        ),
      ).andIsOk.andAssertThatJson {
        node("id").isValidId.satisfies(Consumer { it: BigDecimal -> id = it.toLong() })
      }

    val storage = contentStorageService.get(id!!)
    return storage to result
  }
}
