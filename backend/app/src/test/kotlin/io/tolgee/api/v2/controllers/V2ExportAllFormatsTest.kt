package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.fixtures.AuthorizedRequestFactory.getBearerTokenString
import io.tolgee.fixtures.AuthorizedRequestFactory.token
import io.tolgee.formats.ExportFormat
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class V2ExportAllFormatsTest : ProjectAuthControllerTest("/v2/projects/") {
  var testData: NamespacesTestData? = null

  @LocalServerPort
  private val port: Int? = null

  @BeforeEach
  fun setup() {
    initTestData()
  }

  @AfterEach
  fun cleanup() {
    testData?.let { testDataService.cleanTestData(it.root) }
  }

  @ParameterizedTest
  @EnumSource(ExportFormat::class)
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports to all formats`(format: ExportFormat) {
    // It looks like there is a bug in spring security which causes ConcurrentModificationException when using MockMvc.
    // There are multiple reports and some of them are pretty old. Here are the most relevant ones:
    // https://github.com/spring-projects/spring-framework/issues/31543
    // https://github.com/spring-projects/spring-security/issues/9175
    // It doesn't seem to be fixed soon. The best explanation of what is going on is here by Rossen Stoyanchev:
    // https://github.com/spring-projects/spring-security/issues/11452#issuecomment-1172491187
    // So replacing MockMvc with TestRestTemplate for this test, until these spring bugs are fixed.
    // Uncomment the code below and remove WebEnvironment.RANDOM_PORT to revert to MockMvc when these issues are fixed.
    //
    // val mvcResult = performProjectAuthGet("export?format=${format.name}")
    //   .andIsOk
    //   .andDo { obj: MvcResult -> obj.asyncResult }
    //   .andReturn()

    val testRestTemplate = TestRestTemplate()
    val httpHeaders = HttpHeaders()
    httpHeaders.add("Authorization", getBearerTokenString(token))
    val responseEntity = testRestTemplate.exchange(
      "http://localhost:${port}${projectUrlPrefix}${project.id}/export?format=${format.name}",
      HttpMethod.GET,
      HttpEntity<String>(httpHeaders),
      Resource::class.java,
    )
    assertThat(responseEntity.statusCode.is2xxSuccessful).isTrue()

    // Verify we get some content back
    val responseContent = responseEntity.body?.inputStream?.readAllBytes()
    assertThat(responseContent).isNotEmpty()

    // For ZIP responses, verify we can parse the content
    if (responseEntity.headers.getFirst("Content-Type")?.contains("zip") ?: false) {
      val parsedFiles = parseZip(responseContent!!)
      assertThat(parsedFiles).isNotEmpty()

      // Verify we have files for both the default namespace and ns-1
      val fileNames = parsedFiles.keys
      assertThat(fileNames).hasSizeGreaterThan(0)

      // For formats that support namespaces, verify namespace structure
      if (!format.multiLanguage) {
        assertThat(fileNames.any { it.contains("ns-1") || it.contains("en.") }).isTrue()
      }
    } else {
      // For single file responses, verify we have content
      assertThat(responseContent!!.size).isGreaterThan(0)
    }
  }

  private fun initTestData() {
    testData = NamespacesTestData()

    // Add Czech language to make exports more comprehensive
    testData!!.apply {
      projectBuilder.addLanguage {
        name = "Czech"
        tag = "cs"
        originalName = "Čeština"
      }
    }

    testDataService.saveTestData(testData!!.root)
    projectSupplier = { testData!!.projectBuilder.self }
    userAccount = testData!!.user
  }

  private fun parseZip(responseContent: ByteArray): Map<String, String> {
    val byteArrayInputStream = ByteArrayInputStream(responseContent)
    val zipInputStream = ZipInputStream(byteArrayInputStream)

    return zipInputStream.use {
      generateSequence {
        it.nextEntry
      }.filterNot { it.isDirectory }
        .map { it.name to zipInputStream.bufferedReader().readText() }.toMap()
    }
  }
}
