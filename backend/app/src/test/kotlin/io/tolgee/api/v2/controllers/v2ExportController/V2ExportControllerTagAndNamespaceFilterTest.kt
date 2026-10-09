package io.tolgee.api.v2.controllers.v2ExportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ExportTagAndNamespaceFilterTestData
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MvcResult
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class V2ExportControllerTagAndNamespaceFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ExportTagAndNamespaceFilterTestData

  @BeforeEach
  fun setup() {
    testData = ExportTagAndNamespaceFilterTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `exports keys filtered by included tags, excluded tags and namespaces combined`() {
    val keysByFile =
      performExportPost(
        mapOf(
          "format" to "JSON",
          "zip" to true,
          "filterTagIn" to listOf("included", "feature-*"),
          "filterTagNotIn" to listOf("excluded"),
          "filterNamespace" to listOf("", "ns-1"),
        ),
      )

    keysByFile.assert.containsOnlyKeys("en.json", "ns-1/en.json")
    keysByFile["en.json"].assert.containsExactlyInAnyOrder("default-included", "default-feature")
    keysByFile["ns-1/en.json"].assert.containsExactlyInAnyOrder("ns1-included", "ns1-feature")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `exports keys filtered by excluded tags only within a namespace`() {
    val keysByFile =
      performExportPost(
        mapOf(
          "format" to "JSON",
          "zip" to true,
          "filterTagNotIn" to listOf("excluded"),
          "filterNamespace" to listOf("ns-2"),
        ),
      )

    keysByFile.assert.containsOnlyKeys("ns-2/en.json")
    keysByFile["ns-2/en.json"].assert.containsExactlyInAnyOrder("ns2-included", "ns2-untagged")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns no exported result when wildcard tag matches nothing`() {
    performProjectAuthPost(
      "export",
      mapOf(
        "format" to "JSON",
        "filterTagIn" to listOf("nonexistent-*"),
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.NO_EXPORTED_RESULT)
  }

  private fun performExportPost(body: Any): Map<String, List<String>> {
    val mvcResult =
      performProjectAuthPost("export", body)
        .andIsOk
        .andDo { obj: MvcResult -> obj.asyncResult }
        .andReturn()
    return parseZipKeys(mvcResult.response.contentAsByteArray)
  }

  private fun parseZipKeys(responseContent: ByteArray): Map<String, List<String>> {
    val zipInputStream = ZipInputStream(ByteArrayInputStream(responseContent))
    return zipInputStream.use {
      generateSequence { it.nextEntry }
        .filterNot { it.isDirectory }
        .map { it.name to jacksonObjectMapper().readValue<Map<String, Any?>>(zipInputStream.readBytes()).keys.toList() }
        .toMap()
    }
  }
}
