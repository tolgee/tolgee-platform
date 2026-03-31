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
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import kotlin.time.measureTime

/**
 * Performance regression test for multi-step import into an empty project.
 * Verifies that importing [KEY_COUNT] keys completes within the timeout.
 */
@Disabled
class MultiStepImportPerformanceTest : ProjectAuthControllerTest("/v2/projects/") {
  private val logger = LoggerFactory.getLogger(javaClass)

  companion object {
    const val KEY_COUNT = 30_000
    const val LANGUAGE_COUNT = 3
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
  @Timeout(180)
  @ProjectJWTAuthTestMethod
  fun `import many keys into empty project`() {
    saveAndPrepare()

    val files = generateImportFiles()

    logger.info(
      "Starting multi-step import of {} keys across {} languages ({} total translations)",
      KEY_COUNT,
      LANGUAGE_COUNT,
      KEY_COUNT.toLong() * LANGUAGE_COUNT,
    )

    val totalDuration =
      measureTime {
        // Step 1: Add files
        val addFilesDuration =
          measureTime {
            performAddFiles(files).andIsOk
          }
        logger.info("Step 1 (add files) completed in {}", addFilesDuration)

        // Step 2: Apply import
        val applyDuration =
          measureTime {
            performProjectAuthPut("import/apply?forceMode=OVERRIDE", null).andIsOk
          }
        logger.info("Step 2 (apply) completed in {}", applyDuration)
      }
    logger.info("Total multi-step import completed in {}", totalDuration)

    executeInNewTransaction {
      val keyCount = keyService.getAll(testData.project.id).size
      logger.info("Verified {} keys in project after import", keyCount)
      keyCount.assert.isEqualTo(KEY_COUNT)

      dumpImportActivity()
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

  private fun performAddFiles(files: List<Pair<String, ByteArray>>): ResultActions {
    val builder =
      MockMvcRequestBuilders
        .multipart("/v2/projects/${testData.project.id}/import")

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

    return mvc.perform(AuthorizedRequestFactory.addToken(builder))
  }

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  /**
   * Dumps a deterministic, id-stripped view of the activity tables for the IMPORT revision,
   * suitable for diffing the output between branches.
   *
   * Replaces all numeric ids in JSONB columns and the entity_id column with `<ID>`, then
   * sorts rows so the dump is stable. This is the same data the activity feature reads,
   * minus the volatile primary keys.
   */
  private fun dumpImportActivity() {
    val importTypeName = io.tolgee.activity.data.ActivityType.IMPORT.name

    val revCount =
      entityManager
        .createNativeQuery(
          "SELECT count(*) FROM activity_revision WHERE type = :t",
        ).setParameter("t", importTypeName)
        .singleResult
    val modCount =
      entityManager
        .createNativeQuery(
          """SELECT count(*) FROM activity_modified_entity ame
             JOIN activity_revision ar ON ar.id = ame.activity_revision_id
             WHERE ar.type = :t""",
        ).setParameter("t", importTypeName)
        .singleResult
    val descCount =
      entityManager
        .createNativeQuery(
          """SELECT count(*) FROM activity_describing_entity ade
             JOIN activity_revision ar ON ar.id = ade.activity_revision_id
             WHERE ar.type = :t""",
        ).setParameter("t", importTypeName)
        .singleResult
    logger.info(
      "ACTIVITY STATS (IMPORT only): revisions={}, modified_entities={}, describing_entities={}",
      revCount,
      modCount,
      descCount,
    )

    @Suppress("UNCHECKED_CAST")
    val modifiedRows =
      entityManager
        .createNativeQuery(
          """SELECT entity_class,
                    revision_type,
                    regexp_replace(modifications::text, '\d+', '<ID>', 'g')         AS mods,
                    regexp_replace(describing_data::text, '\d+', '<ID>', 'g')        AS desc_data,
                    regexp_replace(describing_relations::text, '\d+', '<ID>', 'g')   AS desc_rels
             FROM activity_modified_entity ame
             JOIN activity_revision ar ON ar.id = ame.activity_revision_id
             WHERE ar.type = :t
             ORDER BY entity_class, mods, desc_data, desc_rels""",
        ).setParameter("t", importTypeName)
        .resultList as List<Array<Any?>>

    logger.info("=== MODIFIED ENTITIES (IMPORT, id-stripped, sorted) ===")
    modifiedRows.forEach { row ->
      logger.info("MOD: class={} type={} mods={} desc={} rels={}", *row)
    }

    @Suppress("UNCHECKED_CAST")
    val describingRows =
      entityManager
        .createNativeQuery(
          """SELECT entity_class,
                    regexp_replace(data::text, '\d+', '<ID>', 'g')                 AS data,
                    regexp_replace(describing_relations::text, '\d+', '<ID>', 'g')  AS rels
             FROM activity_describing_entity ade
             JOIN activity_revision ar ON ar.id = ade.activity_revision_id
             WHERE ar.type = :t
             ORDER BY entity_class, data, rels""",
        ).setParameter("t", importTypeName)
        .resultList as List<Array<Any?>>

    logger.info("=== DESCRIBING ENTITIES (IMPORT, id-stripped, sorted) ===")
    describingRows.forEach { row ->
      logger.info("DESC: class={} data={} rels={}", *row)
    }
  }
}
