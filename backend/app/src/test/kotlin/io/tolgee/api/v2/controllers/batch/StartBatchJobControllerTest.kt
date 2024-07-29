package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobService
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.ResultActions
import java.util.function.Consumer

@AutoConfigureMockMvc
@ContextRecreatingTest
class StartBatchJobControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: BatchJobsTestData
  var fakeBefore = false

  @Autowired
  lateinit var batchJobOperationQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobService: BatchJobService

  @BeforeEach
  fun setup() {
    batchJobOperationQueue.clear()
    testData = BatchJobsTestData()
    fakeBefore = internalProperties.fakeMtProviders
    internalProperties.fakeMtProviders = true
    machineTranslationProperties.google.apiKey = "mock"
    machineTranslationProperties.google.defaultEnabled = true
    machineTranslationProperties.google.defaultPrimary = true
    machineTranslationProperties.aws.defaultEnabled = false
    machineTranslationProperties.aws.accessKey = "mock"
    machineTranslationProperties.aws.secretKey = "mock"
  }

  @AfterEach
  fun after() {
    internalProperties.fakeMtProviders = fakeBefore
  }

  fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it pre-translates by mt`() {
    val keyCount = 1000
    val keys = testData.addTranslationOperationData(keyCount)
    saveAndPrepare()

    val keyIds = keys.map { it.id }.toList()

    performProjectAuthPost(
      "start-batch-job/pre-translate-by-tm",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to
          listOf(
            testData.projectBuilder.getLanguageByTag("cs")!!.self.id,
            testData.projectBuilder.getLanguageByTag("de")!!.self.id,
          ),
      ),
    )
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
      }

    waitForAllTranslated(keyIds, keyCount, "cs")
    executeInNewTransaction {
      val jobs =
        entityManager.createQuery("""from BatchJob""", BatchJob::class.java)
          .resultList
      jobs.assert.hasSize(1)
      val job = jobs[0]
      job.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      job.activityRevision.assert.isNotNull
      job.activityRevision!!.modifiedEntities.assert.hasSize(2000)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it machine translates`() {
    val keyCount = 1000
    val keys = testData.addTranslationOperationData(keyCount)
    saveAndPrepare()

    val keyIds = keys.map { it.id }.toList()

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to
          listOf(
            testData.projectBuilder.getLanguageByTag("cs")!!.self.id,
            testData.projectBuilder.getLanguageByTag("de")!!.self.id,
          ),
      ),
    )
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
      }

    waitForAllTranslated(keyIds, keyCount)
    executeInNewTransaction {
      val jobs =
        entityManager.createQuery("""from BatchJob""", BatchJob::class.java)
          .resultList
      jobs.assert.hasSize(1)
      val job = jobs[0]
      job.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      job.activityRevision.assert.isNotNull
      job.activityRevision!!.modifiedEntities.assert.hasSize(2000)
    }
  }

  private fun waitForAllTranslated(
    keyIds: List<Long>,
    keyCount: Int,
    expectedCsValue: String = "translated with GOOGLE from en to cs",
  ) {
    waitForNotThrowing(pollTime = 1000, timeout = 60000) {
      @Suppress("UNCHECKED_CAST")
      val czechTranslations =
        entityManager.createQuery(
          """
          from Translation t where t.key.id in :keyIds and t.language.tag = 'cs'
          """.trimIndent(),
        ).setParameter("keyIds", keyIds).resultList as List<Translation>
      czechTranslations.assert.hasSize(keyCount)
      czechTranslations.forEach {
        it.text.assert.contains(expectedCsValue)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it deletes keys`() {
    val keyCount = 100
    val keys = testData.addTranslationOperationData(keyCount)
    saveAndPrepare()

    val keyIds = keys.map { it.id }.toList()

    performProjectAuthPost(
      "start-batch-job/delete-keys",
      mapOf(
        "keyIds" to keyIds,
      ),
    ).andIsOk.waitForJobCompleted()

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all = keyService.getAll(testData.projectBuilder.self.id)
      all.assert.hasSize(1)
    }

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      executeInNewTransaction {
        val data =
          entityManager
            .createQuery("""from BatchJob""", BatchJob::class.java)
            .resultList

        data.assert.hasSize(1)
        data[0].activityRevision.assert.isNotNull
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it changes translation state`() {
    val keyCount = 100
    val keys = testData.addStateChangeData(keyCount)
    saveAndPrepare()

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val allLanguageIds = testData.projectBuilder.data.languages.map { it.self.id }
    val languagesToChangeStateIds = listOf(testData.germanLanguage.id, testData.englishLanguage.id)

    performProjectAuthPost(
      "start-batch-job/set-translation-state",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languagesToChangeStateIds,
        "state" to "REVIEWED",
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all =
        translationService.getTranslations(
          keys.map { it.id },
          allLanguageIds,
        )
      all.count { it.state == TranslationState.REVIEWED }.assert.isEqualTo(keyIds.size * 2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it clears translations`() {
    val keyCount = 1000
    val keys = testData.addStateChangeData(keyCount)
    saveAndPrepare()

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val allLanguageIds = testData.projectBuilder.data.languages.map { it.self.id }
    val languagesToClearIds = listOf(testData.germanLanguage.id, testData.englishLanguage.id)

    performProjectAuthPost(
      "start-batch-job/clear-translations",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languagesToClearIds,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all =
        translationService.getTranslations(
          keys.map { it.id },
          allLanguageIds,
        )
      all.count { it.state == TranslationState.UNTRANSLATED && it.text == null }.assert.isEqualTo(keyIds.size * 2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it copies translations`() {
    val keyCount = 1000
    val keys = testData.addStateChangeData(keyCount)
    saveAndPrepare()

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val allLanguageIds = testData.projectBuilder.data.languages.map { it.self.id }
    val languagesToChangeStateIds = listOf(testData.germanLanguage.id, testData.czechLanguage.id)

    performProjectAuthPost(
      "start-batch-job/copy-translations",
      mapOf(
        "keyIds" to keyIds,
        "sourceLanguageId" to testData.englishLanguage.id,
        "targetLanguageIds" to languagesToChangeStateIds,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all =
        translationService.getTranslations(
          keys.map { it.id },
          allLanguageIds,
        )
      all.count { it.text?.startsWith("en") == true }.assert.isEqualTo(allKeyIds.size + keyIds.size * 2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it validates tag length`() {
    performProjectAuthPost(
      "start-batch-job/tag-keys",
      mapOf(
        "keyIds" to listOf(1),
        "tags" to listOf("a".repeat(101)),
      ),
    ).andIsBadRequest.andPrettyPrint
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it tags keys`() {
    val keyCount = 1000
    val keys = testData.addTagKeysData(keyCount)
    saveAndPrepare()

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(500)
    val newTags = listOf("tag1", "tag3", "a-tag", "b-tag")

    performProjectAuthPost(
      "start-batch-job/tag-keys",
      mapOf(
        "keyIds" to keyIds,
        "tags" to newTags,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all = keyService.getKeysWithTagsById(testData.project.id, keyIds)
      all.assert.hasSize(keyIds.size)
      all.count {
        it.keyMeta?.tags?.map { it.name }?.containsAll(newTags) == true
      }.assert.isEqualTo(keyIds.size)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it untags keys`() {
    val keyCount = 1000
    val keys = testData.addTagKeysData(keyCount)
    saveAndPrepare()

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(300)
    val tagsToRemove = listOf("tag1", "a-tag", "b-tag")

    performProjectAuthPost(
      "start-batch-job/untag-keys",
      mapOf(
        "keyIds" to keyIds,
        "tags" to tagsToRemove,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all = keyService.getKeysWithTagsById(testData.project.id, keyIds)
      all.assert.hasSize(keyIds.size)
      all.count {
        it.keyMeta?.tags?.map { it.name }?.any { tagsToRemove.contains(it) } == false &&
          it.keyMeta?.tags?.map { it.name }?.contains("tag3") == true
      }.assert.isEqualTo(keyIds.size)
    }

    val aKeyId = keyService.get(testData.projectBuilder.self.id, "a-key", null)
    performProjectAuthPost(
      "start-batch-job/untag-keys",
      mapOf(
        "keyIds" to keyIds,
        "tags" to listOf("a-tag"),
      ),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it deletes tags when not used`() {
    val keyCount = 1000
    val keys = testData.addTagKeysData(keyCount)
    saveAndPrepare()

    val aKeyId = keyService.get(testData.projectBuilder.self.id, "a-key", null).id
    performProjectAuthPost(
      "start-batch-job/untag-keys",
      mapOf(
        "keyIds" to listOf(aKeyId),
        "tags" to listOf("a-tag"),
      ),
    ).andIsOk
    waitForNotThrowing { tagService.find(testData.projectBuilder.self, "a-tag").assert.isNull() }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it moves to other namespace`() {
    val keys = testData.addNamespaceData()
    saveAndPrepare()

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(700)

    performProjectAuthPost(
      "start-batch-job/set-keys-namespace",
      mapOf(
        "keyIds" to keyIds,
        "namespace" to "other-namespace",
      ),
    ).andIsOk.waitForJobCompleted()

    val all = keyService.find(keyIds)
    all.count { it.namespace?.name == "other-namespace" }.assert.isEqualTo(keyIds.size)
    namespaceService.find(testData.projectBuilder.self.id, "namespace1").assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it fails on collision when setting namespaces`() {
    testData.addNamespaceData()
    val key = testData.projectBuilder.addKey(keyName = "key").self
    saveAndPrepare()

    val jobId =
      performProjectAuthPost(
        "start-batch-job/set-keys-namespace",
        mapOf(
          "keyIds" to listOf(key.id),
          "namespace" to "namespace",
        ),
      ).andIsOk.waitForJobCompleted().jobId
    keyService.get(key.id).namespace.assert.isNull()
    batchJobService.findJobDto(jobId)?.status.assert.isEqualTo(BatchJobStatus.FAILED)
  }

  fun ResultActions.waitForJobCompleted() =
    andAssertThatJson {
      node("id").isNumber.satisfies(
        Consumer {
          waitFor(pollTime = 2000) {
            val job = batchJobService.findJobDto(it.toLong())
            job?.status?.completed == true
          }
        },
      )
    }

  val ResultActions.jobId: Long
    get() {
      var jobId: Long? = null
      this.andAssertThatJson {
        node("id").isNumber.satisfies(
          Consumer {
            jobId = it.toLong()
          },
        )
      }
      return jobId!!
    }
}
