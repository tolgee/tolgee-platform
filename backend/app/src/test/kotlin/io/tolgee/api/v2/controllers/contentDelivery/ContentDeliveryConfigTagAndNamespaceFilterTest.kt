package io.tolgee.api.v2.controllers.contentDelivery

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.development.testDataBuilder.data.ExportTagAndNamespaceFilterTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsOk
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

@ContextRecreatingTest
class ContentDeliveryConfigTagAndNamespaceFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ExportTagAndNamespaceFilterTestData

  @Autowired
  lateinit var contentDeliveryConfigService: ContentDeliveryConfigService

  @Autowired
  @MockitoSpyBean
  private lateinit var contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider

  @Autowired
  private lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @BeforeEach
  fun setup() {
    batchJobConcurrentLauncher.pause = true
    testData = ExportTagAndNamespaceFilterTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
    Mockito.reset(contentDeliveryFileStorageProvider)
  }

  @AfterEach
  fun after() {
    batchJobConcurrentLauncher.pause = false
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates config with tag and namespace filters and publishes only matching keys`() {
    val id =
      createConfig(
        filterTagIn = listOf("included", "feature-*"),
        filterTagNotIn = listOf("excluded"),
        filterNamespace = listOf("", "ns-1"),
      )

    performProjectAuthGet("content-delivery-configs/$id").andIsOk.andAssertThatJson {
      node("filterTagIn").isEqualTo(listOf("included", "feature-*"))
      node("filterTagNotIn").isEqualTo(listOf("excluded"))
      node("filterNamespace").isEqualTo(listOf("", "ns-1"))
    }

    val keysByFile = publishAndGetStoredKeys(id)

    keysByFile.assert.containsOnlyKeys("en.json", "ns-1/en.json")
    keysByFile["en.json"].assert.containsExactlyInAnyOrder("default-included", "default-feature")
    keysByFile["ns-1/en.json"].assert.containsExactlyInAnyOrder("ns1-included", "ns1-feature")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates tag and namespace filters and publishes only matching keys`() {
    val id =
      createConfig(
        filterTagIn = listOf("included"),
        filterTagNotIn = listOf("feature-*"),
        filterNamespace = listOf("ns-1"),
      )

    performProjectAuthPut(
      "content-delivery-configs/$id",
      mapOf(
        "name" to "Filtered",
        "filterTagIn" to null,
        "filterTagNotIn" to listOf("excluded"),
        "filterNamespace" to listOf("ns-2"),
      ),
    ).andIsOk.andAssertThatJson {
      node("filterTagIn").isNull()
      node("filterTagNotIn").isEqualTo(listOf("excluded"))
      node("filterNamespace").isEqualTo(listOf("ns-2"))
    }

    executeInNewTransaction {
      val config = contentDeliveryConfigService.get(id)
      config.filterTagIn.assert.isNull()
      config.filterTagNotIn.assert.containsExactly("excluded")
      config.filterNamespace.assert.containsExactly("ns-2")
    }

    val keysByFile = publishAndGetStoredKeys(id)

    keysByFile.assert.containsOnlyKeys("ns-2/en.json")
    keysByFile["ns-2/en.json"].assert.containsExactlyInAnyOrder("ns2-included", "ns2-untagged")
  }

  private fun createConfig(
    filterTagIn: List<String>,
    filterTagNotIn: List<String>,
    filterNamespace: List<String>,
  ): Long {
    val response =
      performProjectAuthPost(
        "content-delivery-configs",
        mapOf(
          "name" to "Filtered",
          "filterTagIn" to filterTagIn,
          "filterTagNotIn" to filterTagNotIn,
          "filterNamespace" to filterNamespace,
        ),
      ).andIsOk
        .andAssertThatJson {
          node("filterTagIn").isEqualTo(filterTagIn)
          node("filterTagNotIn").isEqualTo(filterTagNotIn)
          node("filterNamespace").isEqualTo(filterNamespace)
        }.andGetContentAsString
    return jacksonObjectMapper().readValue<Map<String, Any?>>(response)["id"].toString().toLong()
  }

  private fun publishAndGetStoredKeys(id: Long): Map<String, List<String>> {
    val mockedStorage = installDefaultStorageMock()
    performProjectAuthPost("content-delivery-configs/$id").andIsOk
    val slug = contentDeliveryConfigService.get(id).slug
    return Mockito
      .mockingDetails(mockedStorage)
      .invocations
      .filter { it.method.name == "storeFile" }
      .associate { invocation ->
        val path = (invocation.arguments[0] as String).removePrefix("$slug/")
        val keys = jacksonObjectMapper().readValue<Map<String, Any?>>(invocation.arguments[1] as ByteArray).keys
        path to keys.toList()
      }
  }

  private fun installDefaultStorageMock(): FileStorage {
    val fileStorageMock = mock<FileStorage>()
    doReturn(fileStorageMock).whenever(contentDeliveryFileStorageProvider).getContentStorageWithDefaultClient()
    return fileStorageMock
  }
}
