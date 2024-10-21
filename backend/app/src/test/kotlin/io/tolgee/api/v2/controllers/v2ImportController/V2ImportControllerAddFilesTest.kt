package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.dataImport.ImportCleanTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.node
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.InMemoryFileStorage
import io.tolgee.util.performImport
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.servlet.ResultActions
import org.springframework.transaction.annotation.Transactional

@Transactional
class V2ImportControllerAddFilesTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:import/zipOfJsons.zip")
  lateinit var zipOfJsons: Resource

  @Value("classpath:import/zipOfUnknown.zip")
  lateinit var zipOfUnknown: Resource

  @Value("classpath:import/error.json")
  lateinit var errorJson: Resource

  @Value("classpath:import/po/example.po")
  lateinit var poFile: Resource

  @Value("classpath:import/simple.json")
  lateinit var simpleJson: Resource

  @Value("classpath:import/xliff/example.xliff")
  lateinit var xliffFile: Resource

  @Value("classpath:import/tooLongTranslation.json")
  lateinit var tooLongTranslation: Resource

  @Value("classpath:import/tooLongErrorParamValue.json")
  lateinit var tooLongErrorParamValue: Resource

  @Value("classpath:import/namespaces.zip")
  lateinit var namespacesZip: Resource

  @Value("classpath:import/namespacesMac.zip")
  lateinit var namespacesMacZip: Resource

  @Value("classpath:import/importWithConflicts.zip")
  lateinit var importWithConflicts: Resource

  @Value("classpath:import/empty-keys.json")
  lateinit var emptyKeys: Resource

  @Value("classpath:import/nested.json")
  lateinit var nested: Resource

  @AfterEach
  fun resetProps() {
    tolgeeProperties.maxTranslationTextLength = 10000
    (fileStorage as InMemoryFileStorage).clear()
    tolgeeProperties.import.storeFilesForDebugging = false
  }

  @Test
  fun `it parses zip file stores it for debugging and saves issues`() {
    tolgeeProperties.import.storeFilesForDebugging = true
    val base = dbPopulator.createBase(generateUniqueString())
    commitTransaction()

    val fileName = "zipOfUnknown.zip"
    performImport(projectId = base.project.id, listOf(Pair(fileName, zipOfUnknown))).andAssertThatJson {
      node("errors[2].code").isEqualTo("cannot_parse_file")
    }

    doesStoredFileExists(fileName, base.project.id).assert.isTrue()
  }

  @Test
  fun `doesn't store file for when disabled`() {
    val base = dbPopulator.createBase(generateUniqueString())
    commitTransaction()

    val fileName = "zipOfUnknown.zip"
    performImport(projectId = base.project.id, listOf(Pair(fileName, zipOfUnknown)))
    doesStoredFileExists(fileName, base.project.id).assert.isFalse()
  }

  fun doesStoredFileExists(
    fileName: String,
    projectId: Long,
  ): Boolean {
    val import = importService.find(projectId, userAccount!!.id)
    return fileStorage.fileExists("importFiles/${import!!.id}/$fileName")
  }

  @Test
  fun `it handles po file`() {
    val base = dbPopulator.createBase(generateUniqueString())

    performImport(projectId = base.project.id, listOf(Pair("example.po", poFile)))
      .andPrettyPrint.andAssertThatJson {
        node("result._embedded.languages").isArray.hasSize(1)
      }.andReturn()

    entityManager.clear()

    importService.find(base.project.id, base.userAccount.id)?.let {
      assertThat(it.files).hasSize(1)
      assertThat(it.files[0].languages[0].translations).hasSize(8)
      // correctly assigns isPlural
      assertThat(it.files[0].keys[4].translations[0].isPlural).isTrue()
      assertThat(it.files[0].keys[3].translations[0].isPlural).isFalse()
    }
  }

  @Test
  fun `it handles xliff file`() {
    val base = dbPopulator.createBase(generateUniqueString())

    performImport(projectId = base.project.id, listOf(Pair("example.xliff", xliffFile)))
      .andPrettyPrint.andAssertThatJson {
        node("result._embedded.languages").isArray.hasSize(2)
      }.andReturn()
  }

  @Test
  fun `it returns error when json could not be parsed`() {
    val base = dbPopulator.createBase(generateUniqueString())

    performImport(projectId = base.project.id, listOf(Pair("error.json", errorJson)))
      .andIsOk.andAssertThatJson {
        node("errors[0].code").isEqualTo("cannot_parse_file")
        node("errors[0].params[0]").isEqualTo("error.json")
        node("errors[0].params[1]").isString.contains("Unrecognized token")
      }
  }

  @Test
  fun `it throws when more then 100 languages`() {
    val base = dbPopulator.createBase(generateUniqueString())

    val data = (1..101).map { "simple$it.json" to simpleJson }

    performImport(projectId = base.project.id, data)
      .andIsBadRequest.andPrettyPrint.andAssertThatJson {
        node("code").isEqualTo("cannot_add_more_then_100_languages")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it imports empty keys`() {
    performImport(projectId = project.id, listOf("empty-keys.json" to emptyKeys))
      .andIsOk.andPrettyPrint

    entityManager.clear()

    importService.find(project.id, userAccount!!.id)?.let {
      assertThat(it.files[0].keys).hasSize(1)
    }

    val path = "import/apply?forceMode=OVERRIDE"
    performProjectAuthPut(path, null).andIsOk
  }

  @Test
  fun `it imports nested keys with provided delimiter`() {
    val base = dbPopulator.createBase(generateUniqueString())

    performImport(projectId = base.project.id, listOf("nested.json" to nested), mapOf("structureDelimiter" to ";"))
      .andIsOk

    entityManager.clear()

    importService.find(base.project.id, base.userAccount.id)?.let {
      it.files[0].keys.find { it.name == "this;nested;a" }.assert.isNotNull
    }
  }

  @Test
  fun `it saves proper data and returns correct response`() {
    val base = dbPopulator.createBase(generateUniqueString())
    commitTransaction()

    performImport(projectId = base.project.id, listOf(Pair("zipOfJsons.zip", zipOfJsons)))
      .andAssertThatJson {
        node("result._embedded.languages").isArray.hasSize(3)
      }
    validateSavedJsonImportData(base.project, base.userAccount)
  }

  @Test
  fun `it adds a file with long translation text and stores issues`() {
    val base = dbPopulator.createBase(generateUniqueString())
    commitTransaction()
    tolgeeProperties.maxTranslationTextLength = 20

    executeInNewTransaction {
      performImport(
        projectId = base.project.id,
        listOf(Pair("tooLongTranslation.json", tooLongTranslation)),
      ).andIsOk
    }

    executeInNewTransaction {
      importService.find(base.project.id, base.userAccount.id)?.let {
        assertThat(it.files).hasSize(1)
        assertThat(it.files[0].issues).hasSize(1)
        assertThat(it.files[0].issues[0].type).isEqualTo(FileIssueType.TRANSLATION_TOO_LONG)
        assertThat(it.files[0].issues[0].params?.get(0)?.value).isEqualTo("too_long")
      }
    }
  }

  @Test
  fun `gracefully handles missing files part`() {
    val base = dbPopulator.createBase(generateUniqueString())
    commitTransaction()

    executeInNewTransaction {
      performImport(
        projectId = base.project.id,
        null,
      ).andIsBadRequest.andAssertThatJson {
        node("STANDARD_VALIDATION") {
          node("files").isEqualTo("Required part 'files' is not present.")
        }
      }
    }
  }

  @Test
  fun `pre-selects namespaces and languages correctly`() {
    val base = dbPopulator.createBase(generateUniqueString())
    commitTransaction()
    tolgeeProperties.maxTranslationTextLength = 20

    executeInNewTransaction {
      performImport(
        projectId = base.project.id,
        listOf(Pair("namespaces.zip", namespacesZip)),
      ).andIsOk
    }

    executeInNewTransaction {
      importService.find(base.project.id, base.userAccount.id)?.let {
        assertThat(it.files).hasSize(4)
        val homepageEn = it.files.find { it.namespace == "homepage" && it.name == "homepage/en.json" }
        homepageEn!!.languages[0].existingLanguage?.tag.assert.isEqualTo("en")
        val movies = it.files.find { it.namespace == "movies" && it.name == "movies/de.json" }
        movies!!.languages[0].existingLanguage?.tag.assert.isEqualTo("de")
      }
    }
  }

  @Test
  fun `works fine with Mac generated zip`() {
    val base = dbPopulator.createBase(generateUniqueString())
    commitTransaction()
    tolgeeProperties.maxTranslationTextLength = 20

    executeInNewTransaction {
      performImport(
        projectId = base.project.id,
        listOf(Pair("namespaces.zip", namespacesMacZip)),
      ).andIsOk
    }

    executeInNewTransaction {
      importService.find(base.project.id, base.userAccount.id)?.let {
        assertThat(it.files).hasSize(4)
      }
    }
  }

  @Test
  fun `correctly computes conflicts on import`() {
    val testData = ImportCleanTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.userAccount.username)
    executeInNewTransaction {
      performImport(
        projectId = testData.project.id,
        listOf(Pair("importWithConflicts.zip", importWithConflicts)),
      ).andIsOk.andAssertThatJson {
        node("result.page.totalElements").isEqualTo(2)
        node("result._embedded.languages[0].conflictCount").isEqualTo(1)
        node("result._embedded.languages[1].conflictCount").isEqualTo(1)
      }
    }
  }

  @Test
  fun `correctly reports collisions between files`() {
    val testData = ImportCleanTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.userAccount.username)
    performImport(
      projectId = testData.project.id,
      listOf(
        Pair("en.json", simpleJson),
      ),
    ).andIsOk
    performImport(
      projectId = testData.project.id,
      listOf(
        Pair("en.json", simpleJson),
      ),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("result._embedded.languages[1].importFileIssueCount").isEqualTo(1)
    }
  }

  private fun validateSavedJsonImportData(
    project: Project,
    userAccount: UserAccount,
  ) {
    importService.find(project.id, userAccount.id)!!.let { importEntity ->
      entityManager.refresh(importEntity)
      assertThat(importEntity.files.size).isEqualTo(3)
      val expectedFiles = listOf("en.json", "cs.json", "fr.json")
      assertThat(importEntity.files.map { it.name }).containsAll(expectedFiles)
      val keys = importService.findKeys(importEntity)
      keys.forEach { key ->
        assertThat(keys.filter { it.name == key.name })
          .describedAs("Each key is stored at max files.size times")
          .hasSizeLessThan(expectedFiles.size + 1)
      }
      importEntity.files.forEach {
        assertThat(it.issues).hasSize(0)
      }
      assertThat(keys).hasSize(540)
    }
  }

  private fun performImport(
    projectId: Long,
    files: List<Pair<String, Resource>>?,
    params: Map<String, Any?> = mapOf(),
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return performImport(mvc, projectId, files, params)
  }
}
