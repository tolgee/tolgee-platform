package io.tolgee.api.v2.controllers.v2ImportController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.enums.TranslationState
import io.tolgee.repository.activity.ActivityModifiedEntityRepository
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class SingleStepImportActivityTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var activityModifiedEntityRepository: ActivityModifiedEntityRepository

  private lateinit var testData: BaseTestData

  @BeforeEach
  fun beforeEach() {
    testData = BaseTestData()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records ADD activity for newly created key and translation`() {
    saveAndPrepare()

    importJson("en.json", mapOf("hello" to "Hello")).andIsOk

    val (revision, modified) = getImportRevisionAndModified()
    revision.type.assert.isEqualTo(ActivityType.IMPORT)

    val keyRow = modified.singleByClass("Key")
    keyRow.revisionType.assert.isEqualTo(RevisionType.ADD)
    keyRow.modifications["name"]!!
      .new.assert
      .isEqualTo("hello")
    keyRow.modifications
      .containsKey("isPlural")
      .assert
      .isTrue()

    val translationRow = modified.singleByClass("Translation")
    translationRow.revisionType.assert.isEqualTo(RevisionType.ADD)
    translationRow.modifications["text"]!!
      .new.assert
      .isEqualTo("Hello")
    translationRow.modifications
      .containsKey("state")
      .assert
      .isTrue()
    translationRow.describingRelations.assert.isNotNull()
    translationRow.describingRelations!!["key"]!!
      .entityClass.assert
      .isEqualTo("Key")
    translationRow.describingRelations!!["language"]!!
      .entityClass.assert
      .isEqualTo("Language")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records ADD activity for translation only when key already exists`() {
    val germanLanguage = testData.projectBuilder.addGerman().self
    val existingKey = testData.projectBuilder.addKey { name = "greeting" }.self
    testData.projectBuilder.addTranslation {
      key = existingKey
      language = testData.englishLanguage
      text = "Hello"
    }
    saveAndPrepare()

    importJson(
      "de.json",
      mapOf("greeting" to "Hallo"),
    ).andIsOk

    val (_, modified) = getImportRevisionAndModified()

    modified.filter { it.entityClass == "Key" }.assert.isEmpty()

    val translationRow = modified.singleByClass("Translation")
    translationRow.revisionType.assert.isEqualTo(RevisionType.ADD)
    translationRow.modifications["text"]!!
      .new.assert
      .isEqualTo("Hallo")
    translationRow.entityId.assert.isNotEqualTo(0L)
    // describing relation should reference the existing key, not a new one
    translationRow.describingRelations!!["key"]!!
      .entityId.assert
      .isEqualTo(existingKey.id)
    translationRow.describingRelations!!["language"]!!
      .entityId.assert
      .isEqualTo(germanLanguage.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records MOD activity when overriding existing translation`() {
    val germanLanguage = testData.projectBuilder.addGerman().self
    val key = testData.projectBuilder.addKey { name = "greeting" }.self
    testData.projectBuilder.addTranslation {
      this.key = key
      this.language = germanLanguage
      this.text = "Old hallo"
    }
    saveAndPrepare()

    importJson(
      "de.json",
      mapOf("greeting" to "Hallo"),
      params = mapOf("forceMode" to "OVERRIDE"),
    ).andIsOk

    val (_, modified) = getImportRevisionAndModified()

    // Key was untouched
    modified.filter { it.entityClass == "Key" }.assert.isEmpty()

    val translationRow = modified.singleByClass("Translation")
    translationRow.revisionType.assert.isEqualTo(RevisionType.MOD)
    translationRow.modifications["text"]!!
      .old.assert
      .isEqualTo("Old hallo")
    translationRow.modifications["text"]!!
      .new.assert
      .isEqualTo("Hallo")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records ADD activity for plural key`() {
    saveAndPrepare()

    importJson(
      "en.json",
      mapOf("items" to "{count, plural, one {# item} other {# items}}"),
    ).andIsOk

    val (_, modified) = getImportRevisionAndModified()
    val keyRow = modified.singleByClass("Key")
    keyRow.revisionType.assert.isEqualTo(RevisionType.ADD)
    keyRow.modifications["name"]!!
      .new.assert
      .isEqualTo("items")
    keyRow.modifications["isPlural"]!!
      .new.assert
      .isEqualTo(true)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records ADD activity for namespace when importing into new namespace`() {
    enableNamespaces()
    saveAndPrepare()

    importJson(
      "en.json",
      mapOf("test" to "Test"),
      params =
        mapOf(
          "fileMappings" to
            listOf(
              mapOf(
                "fileName" to "en.json",
                "namespace" to "feature-x",
              ),
            ),
        ),
    ).andIsOk

    val (_, modified) = getImportRevisionAndModified()

    val namespaceRow = modified.singleByClass("Namespace")
    namespaceRow.revisionType.assert.isEqualTo(RevisionType.ADD)
    namespaceRow.modifications["name"]!!
      .new.assert
      .isEqualTo("feature-x")

    // The new key should reference the new namespace
    val keyRow = modified.singleByClass("Key")
    keyRow.modifications["namespace"]!!
      .new.assert
      .isNotNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records ADD activity for many keys in a single import`() {
    saveAndPrepare()

    val keyCount = 25
    val payload =
      LinkedHashMap<String, String>(keyCount).apply {
        for (i in 1..keyCount) put("k$i", "v$i")
      }

    importJson("en.json", payload).andIsOk

    val (revision, modified) = getImportRevisionAndModified()
    revision.type.assert.isEqualTo(ActivityType.IMPORT)
    modified
      .filter { it.entityClass == "Key" }
      .size.assert
      .isEqualTo(keyCount)
    modified
      .filter { it.entityClass == "Translation" }
      .size.assert
      .isEqualTo(keyCount)
    modified
      .filter { it.entityClass == "Translation" }
      .all { it.revisionType == RevisionType.ADD }
      .assert
      .isTrue()

    // The last key in the import must still produce an activity row
    val lastKey =
      modified.single {
        it.entityClass == "Key" && it.modifications["name"]?.new == "k25"
      }
    lastKey.revisionType.assert.isEqualTo(RevisionType.ADD)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records KeyMeta tag activity when tagNewKeys is set`() {
    saveAndPrepare()

    importJson(
      "en.json",
      mapOf("hello" to "Hello"),
      params = mapOf("tagNewKeys" to listOf("new-tag")),
    ).andIsOk

    val (_, modified) = getImportRevisionAndModified()

    modified
      .singleByClass("Key")
      .modifications["name"]!!
      .new.assert
      .isEqualTo("hello")

    val keyMetaRow = modified.singleByClass("KeyMeta")
    keyMetaRow.modifications
      .containsKey("tags")
      .assert
      .isTrue()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records state reset when overriding REVIEWED translation`() {
    val germanLanguage = testData.projectBuilder.addGerman().self
    val key = testData.projectBuilder.addKey { name = "greeting" }.self
    testData.projectBuilder.addTranslation {
      this.key = key
      this.language = germanLanguage
      this.text = "Old hallo"
      this.state = TranslationState.REVIEWED
    }
    saveAndPrepare()

    importJson(
      "de.json",
      mapOf("greeting" to "Hallo"),
      params = mapOf("forceMode" to "OVERRIDE"),
    ).andIsOk

    val (_, modified) = getImportRevisionAndModified()
    val translationRow = modified.singleByClass("Translation")
    translationRow.revisionType.assert.isEqualTo(RevisionType.MOD)
    translationRow.modifications["text"]!!
      .old.assert
      .isEqualTo("Old hallo")
    translationRow.modifications["text"]!!
      .new.assert
      .isEqualTo("Hallo")
    // State should record the reset from REVIEWED -> TRANSLATED
    translationRow.modifications
      .containsKey("state")
      .assert
      .isTrue()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records DEL activity when removeOtherKeys deletes existing keys`() {
    val obsoleteKey = testData.projectBuilder.addKey { name = "obsolete" }.self
    testData.projectBuilder.addTranslation {
      this.key = obsoleteKey
      this.language = testData.englishLanguage
      this.text = "to be removed"
    }
    saveAndPrepare()

    importJson(
      "en.json",
      mapOf("kept" to "Kept"),
      params = mapOf("removeOtherKeys" to true),
    ).andIsOk

    val (_, modified) = getImportRevisionAndModified()

    val addedKey = modified.single { it.entityClass == "Key" && it.revisionType == RevisionType.ADD }
    addedKey.modifications["name"]!!
      .new.assert
      .isEqualTo("kept")

    val deletedKey = modified.single { it.entityClass == "Key" && it.revisionType == RevisionType.DEL }
    deletedKey.entityId.assert.isEqualTo(obsoleteKey.id)
  }

  private fun List<ActivityModifiedEntity>.singleByClass(entityClass: String): ActivityModifiedEntity =
    filter { it.entityClass == entityClass }.let {
      it.size.assert
        .describedAs("expected single $entityClass row, got ${it.size}")
        .isEqualTo(1)
      it.single()
    }

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  private fun enableNamespaces() {
    testData.projectBuilder.self.useNamespaces = true
  }

  private fun getImportRevisionAndModified(): Pair<ActivityRevision, List<ActivityModifiedEntity>> =
    executeInNewTransaction {
      val revision =
        entityManager
          .createQuery(
            "from ActivityRevision ar where ar.type = :type order by ar.id desc",
            ActivityRevision::class.java,
          ).setParameter("type", ActivityType.IMPORT)
          .resultList
          .firstOrNull()
          ?: throw AssertionError("No IMPORT activity revision found")
      revision to activityModifiedEntityRepository.findByRevisionId(revision.id)
    }

  private fun importJson(
    fileName: String,
    content: Map<String, Any?>,
    params: Map<String, Any?> = mapOf(),
  ): ResultActions {
    val mapper = jacksonObjectMapper()
    val builder =
      MockMvcRequestBuilders
        .multipart("/v2/projects/${testData.project.id}/single-step-import")
    builder.file(
      MockMultipartFile(
        "files",
        fileName,
        "application/json",
        mapper.writeValueAsBytes(content),
      ),
    )
    builder.part(MockPart("params", mapper.writeValueAsBytes(params)))
    return mvc.perform(AuthorizedRequestFactory.addToken(builder))
  }
}
