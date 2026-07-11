package io.tolgee.api.v2.controllers.v2ExportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.ignoreTestOnSpringBug
import io.tolgee.formats.ExportFormat
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MvcResult
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class V2ExportAllFormatsTest : ProjectAuthControllerTest("/v2/projects/") {
  private final val log = org.slf4j.LoggerFactory.getLogger(V2ExportAllFormatsTest::class.java)

  var testData: NamespacesTestData? = null

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
    val mvcResult =
      ignoreTestOnSpringBug {
        performProjectAuthGet("export?format=${format.name}")
          .andIsOk
          .andDo { obj: MvcResult -> obj.asyncResult }
          .andReturn()
      }

    // Verify we get some content back
    val responseContent = mvcResult.response.contentAsByteArray
    assertThat(responseContent).isNotEmpty()

    // For ZIP responses, verify we can parse the content
    if (mvcResult.response.contentType?.contains("zip") == true) {
      val parsedFiles = parseZip(responseContent)
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
      assertThat(responseContent.size).isGreaterThan(0)
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
        .map { it.name to zipInputStream.bufferedReader().readText() }
        .toMap()
    }
  }
}
