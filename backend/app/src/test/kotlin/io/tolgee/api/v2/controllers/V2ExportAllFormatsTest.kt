package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.formats.ExportFormat
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.opentest4j.TestAbortedException
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
    val mvcResult = try {
      performProjectAuthGet("export?format=${format.name}")
        .andIsOk
        .andDo { obj: MvcResult -> obj.asyncResult }
        .andReturn()
    } catch (e: ConcurrentModificationException) {
      if (isSpringBug(e)) {
        log.error(e.message, e)
        // Retrying the request once more can still lead to the bug, plus it will hide
        // this dirty "fix" completely. So TestAbortedException is chosen to mark the test
        // as ignored. This way it won't fail the cicd pipeline, and you still see it in the final
        // report, that it was ignored.
        throw TestAbortedException("spring-security/issues/9175", e)
      } else {
        throw e
      }
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

  /**
   * Main link to the spring bug
   * https://github.com/spring-projects/spring-security/issues/9175
   * It doesn't seem to be fixed soon. The best explanation of what is going on is here by Rossen Stoyanchev:
   * https://github.com/spring-projects/spring-security/issues/11452#issuecomment-1172491187
   *
   * You can see in this method where the ConcurrentModification actually happens ("addHeader" <-> "setHeader").
   * Also for more details check the pull request discussion: https://github.com/tolgee/tolgee-platform/pull/3233
   */
  private fun isSpringBug(e: ConcurrentModificationException): Boolean {
    return e.stackTrace.first().className.contains("HashMap") &&
        e.stackTrace.first().methodName == "computeIfAbsent" &&
        e.stackTrace.find {
      it.className.contains("HttpServletResponseWrapper") &&
          (it.methodName == "addHeader" || it.methodName == "setHeader")
        } != null
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
