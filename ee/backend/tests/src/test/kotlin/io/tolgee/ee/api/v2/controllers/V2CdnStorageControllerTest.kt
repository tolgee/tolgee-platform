package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.cdn.CdnFileStorageProvider
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.service.CdnStorageService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.data.domain.Pageable
import org.springframework.test.web.servlet.ResultActions
import java.math.BigDecimal
import java.util.function.Consumer

class V2CdnStorageControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  @Autowired
  private lateinit var cdnStorageService: CdnStorageService

  @SpyBean
  @Autowired
  private lateinit var cdnFileStorageProvider: CdnFileStorageProvider

  @BeforeEach
  fun beforeEach() {
    val testData = BaseTestData()
    Mockito.reset(cdnFileStorageProvider)
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates CDN storage`() {
    performCreate()
    executeInNewTransaction {
      val all = cdnStorageService.getAllInProject(project.id, Pageable.ofSize(100))
      all.assert.hasSize(1)
      val azureCdnConfig = all.content.single().azureCdnConfig!!
      azureCdnConfig.connectionString.assert.isEqualTo("fakeConnectionString")
      azureCdnConfig.containerName.assert.isEqualTo("fakeContainerName")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists storages`() {
    performCreate()
    performProjectAuthGet("cdn-storages").andAssertThatJson {
      node("_embedded.cdnStorages").isArray.hasSize(1)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns single storage`() {
    val (storage) = performCreate()
    performProjectAuthGet("cdn-storages/${storage.id}").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Azure")
    }
  }


  @Test
  @ProjectJWTAuthTestMethod
  fun `updates CDN storage`() {
    val (storage) = performCreate()
    performProjectAuthPut(
      "cdn-storages/${storage.id}", mapOf(
        "name" to "S3",
        "s3CdnConfig" to mapOf(
          "bucketName" to "bucketName",
          "accessKey" to "accessKey",
          "secretKey" to "secretKey",
          "endpoint" to "endpoint",
          "signingRegion" to "signingRegion",
        )
      )
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("S3")
    }

    executeInNewTransaction {
      val updatedStorage = cdnStorageService.get(storage.id)
      updatedStorage.s3CdnConfig!!.bucketName.assert.isEqualTo("bucketName")
      updatedStorage.azureCdnConfig.assert.isNull()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `validates Azure Config storage`() {
    performProjectAuthPost(
      "cdn-storages",
      mapOf(
        "azureCdnConfig" to mapOf(
          "connectionString" to "fakeConnectionString",
          "containerName" to "fakeContainerName"
        )
      )
    ).andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes an storage`() {
    val (storage) = performCreate()
    performProjectAuthDelete(
      "cdn-storages/${storage.id}"
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tests azure storage`() {
    performProjectAuthPost(
      "cdn-storages/test",
      mapOf(
        "name" to "azure",
        "azureCdnConfig" to mapOf(
          "connectionString" to "fakeConnectionString",
          "containerName" to "fakeContainerName"
        )
      )
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tests s3 storage`() {
    performProjectAuthPost(
      "cdn-storages/test",
      mapOf(
        "name" to "s3",
        "s3CdnConfig" to mapOf(
          "bucketName" to "bucketName",
          "accessKey" to "accessKey",
          "secretKey" to "secretKey",
          "endpoint" to "endpoint",
          "signingRegion" to "signingRegion",
        )
      )
    ).andIsOk
  }

  private fun performCreate(): Pair<CdnStorage, ResultActions> {
    doAnswer {
      mock<AzureBlobFileStorage>()
    }.whenever(cdnFileStorageProvider).getStorage(any());

    var id: Long? = null

    val result = performProjectAuthPost(
      "cdn-storages",
      mapOf(
        "name" to "Azure",
        "azureCdnConfig" to mapOf(
          "connectionString" to "fakeConnectionString",
          "containerName" to "fakeContainerName"
        )
      )
    ).andIsOk.andAssertThatJson {
      node("id").isValidId.satisfies(Consumer { it: BigDecimal -> id = it.toLong() })
    }

    val storage = cdnStorageService.get(id!!)
    return storage to result
  }
}
