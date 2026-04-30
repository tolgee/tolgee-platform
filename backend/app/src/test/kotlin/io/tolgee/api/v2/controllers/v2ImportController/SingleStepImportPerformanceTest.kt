package io.tolgee.api.v2.controllers.v2ImportController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import kotlin.time.measureTime

/**
 * Performance regression test for single-step import into an empty project.
 * Verifies that importing [KEY_COUNT] keys completes within the timeout.
 */
@Disabled
class SingleStepImportPerformanceTest : ProjectAuthControllerTest("/v2/projects/") {
  private val logger = LoggerFactory.getLogger(javaClass)

  companion object {
    /**
     * Number of keys to import. Adjust this to find the threshold where import
     * takes ~1 minute or fails.
     */
    const val KEY_COUNT = 30_000
    const val LANGUAGE_COUNT = 5
  }

  lateinit var testData: BaseTestData

  @BeforeEach
  fun beforeEach() {
    testData = BaseTestData()
    for (i in 2..LANGUAGE_COUNT) {
      testData.projectBuilder.addLanguage {
        name = "Language $i"
        tag = "lang-$i"
        originalName = "Language $i"
      }
    }
  }

  @Test
  @Timeout(60)
  @ProjectJWTAuthTestMethod
  fun `import many keys into empty project`() {
    saveAndPrepare()

    val files = generateImportFiles()

    logger.info(
      "Starting import of {} keys across {} languages ({} total translations)",
      KEY_COUNT,
      LANGUAGE_COUNT,
      KEY_COUNT.toLong() * LANGUAGE_COUNT,
    )

    val duration =
      measureTime {
        performSingleStepImport(files).andIsOk
      }
    logger.info("Import completed in {}", duration)

    executeInNewTransaction {
      val keyCount = keyService.getAll(testData.project.id).size
      logger.info("Verified {} keys in project after import", keyCount)
      keyCount.assert.isEqualTo(KEY_COUNT)
    }
  }

  private fun generateImportFiles(): List<Pair<String, ByteArray>> {
    val languageTags = mutableListOf("en")
    for (i in 2..LANGUAGE_COUNT) {
      languageTags.add("lang-$i")
    }

    return languageTags.map { tag ->
      tag to buildJsonContent(tag)
    }
  }

  private fun buildJsonContent(languageTag: String): ByteArray {
    val map = LinkedHashMap<String, String>(KEY_COUNT)
    for (i in 1..KEY_COUNT) {
      map["key_$i"] = "value_${languageTag}_$i"
    }
    return jacksonObjectMapper().writeValueAsBytes(map)
  }

  private fun performSingleStepImport(files: List<Pair<String, ByteArray>>): ResultActions {
    val builder =
      MockMvcRequestBuilders
        .multipart("/v2/projects/${testData.project.id}/single-step-import")

    files.forEach { (fileName, content) ->
      builder.file(
        MockMultipartFile(
          "files",
          "$fileName.json",
          "application/json",
          content,
        ),
      )
    }

    val params =
      mapOf(
        "forceMode" to "OVERRIDE",
        "createNewKeys" to true,
      )
    builder.part(MockPart("params", jacksonObjectMapper().writeValueAsBytes(params)))

    return mvc.perform(AuthorizedRequestFactory.addToken(builder))
  }

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }
}
