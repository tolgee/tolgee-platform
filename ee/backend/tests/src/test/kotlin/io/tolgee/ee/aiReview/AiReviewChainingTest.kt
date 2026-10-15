package io.tolgee.ee.aiReview

import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobCreated
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.constants.Feature
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.service.aiReview.AiReviewTriggerService
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import java.util.concurrent.CopyOnWriteArrayList

class AiReviewChainingTest : AbstractAiReviewTest() {
  data class RecordedEvent(
    val kind: String,
    val type: BatchJobType,
    val jobId: Long,
    val target: List<Any>,
  )

  class BatchJobEventRecorder {
    val events = CopyOnWriteArrayList<RecordedEvent>()

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun onCreated(event: OnBatchJobCreated) {
      events.add(RecordedEvent(CREATED, event.job.type, event.job.id, event.job.target))
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun onFinalized(event: OnBatchJobFinalized) {
      events.add(RecordedEvent(FINALIZED, event.job.type, event.job.id, event.job.target))
    }
  }

  @TestConfiguration
  class RecorderConfiguration {
    @Bean
    fun batchJobEventRecorder() = BatchJobEventRecorder()
  }

  @Autowired
  lateinit var recorder: BatchJobEventRecorder

  @Autowired
  lateinit var triggerService: AiReviewTriggerService

  @Autowired
  lateinit var translationQaIssueRepository: TranslationQaIssueRepository

  override val enabledFeatures = setOf(Feature.AI_REVIEW, Feature.QA_CHECKS)

  @BeforeEach
  fun setupChaining() {
    recorder.events.clear()
    testData.project.useQaChecks = true
  }

  @AfterEach
  fun cleanupChaining() {
    internalProperties.fakeMtProviders = false
  }

  @Test
  fun `starts AI review only after QA finished for a human edit`() {
    testData.addRule { skipWithOpenQaIssues = false }
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
    assertAiReviewCreatedAfterFinalizationOf(BatchJobType.QA_CHECK, testData.german.self.id)
  }

  @Test
  fun `chains auto translation, QA and AI review`() {
    enableFakeMt()
    testData.enableAutoTranslation()
    testData.addRule {
      tagIn = mutableListOf("ui")
      skipWithOpenQaIssues = false
    }
    saveTestData()

    val translationId = createUiKeyAndGetGermanTranslationId()

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
    recorder.events
      .any { it.kind == FINALIZED && it.type == BatchJobType.AUTO_TRANSLATE }
      .assert
      .isTrue()
    assertAiReviewCreatedAfterFinalizationOf(BatchJobType.QA_CHECK, testData.german.self.id)
  }

  @Test
  fun `starts AI review right after auto translation when QA is off for the language`() {
    enableFakeMt()
    testData.enableAutoTranslation()
    testData.german.setQaConfig { enabled = false }
    testData.addRule {
      tagIn = mutableListOf("ui")
      skipWithOpenQaIssues = false
    }
    saveTestData()

    val translationId = createUiKeyAndGetGermanTranslationId()

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    assertAiReviewCreatedAfterFinalizationOf(BatchJobType.AUTO_TRANSLATE, testData.german.self.id)
    recorder.events
      .filter { it.type == BatchJobType.QA_CHECK }
      .flatMap { it.targetItems() }
      .map { it.languageId }
      .assert
      .doesNotContain(testData.german.self.id)
  }

  @Test
  fun `skips translations with open QA issues`() {
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen!!!")

    waitForProjectJobsCompleted()
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      waitForProjectJobsCompleted()
      recorder.events
        .any { it.kind == FINALIZED && it.type == BatchJobType.AI_REVIEW }
        .assert
        .isTrue()
    }
    executeInNewTransaction(platformTransactionManager) {
      translationQaIssueRepository
        .countByTranslationIdAndState(translationId, QaIssueState.OPEN)
        .assert
        .isGreaterThan(0)
    }
    findReview(translationId).assert.isNull()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `QA finished for an outdated text does not trigger AI review`() {
    testData.addRule()
    testData.uiGerman.self.qaChecksStale = true
    testData.uiGerman.addAiReview {
      outcome = AiReviewOutcome.PENDING
      textHash = aiReviewTextHash("Jetzt zahlen")
    }
    saveTestData()
    val target = BatchTranslationTargetItem(testData.uiKey.self.id, testData.german.self.id)

    executeInNewTransaction(platformTransactionManager) {
      triggerService.startReviewAfterQa(projectId, listOf(target))
    }
    aiReviewJobs().assert.isEmpty()

    executeInNewTransaction(platformTransactionManager) {
      translationService.get(testData.uiGerman.self.id).qaChecksStale = false
    }
    executeInNewTransaction(platformTransactionManager) {
      triggerService.startReviewAfterQa(projectId, listOf(target))
    }
    aiReviewJobs().assert.hasSize(1)
    waitForReview(testData.uiGerman.self.id) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
  }

  private fun enableFakeMt() {
    initMachineTranslationProperties(-1, enabledServices = setOf(MtServiceType.GOOGLE))
    internalProperties.fakeMtProviders = true
  }

  private fun createUiKeyAndGetGermanTranslationId(): Long {
    performAuthPost(
      "/v2/projects/$projectId/keys",
      CreateKeyDto(name = "new-ui-key", translations = mapOf("en" to "Save changes"), tags = listOf("ui")),
    ).andIsCreated

    var translationId: Long? = null
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      executeInNewTransaction(platformTransactionManager) {
        val key = keyService.get(projectId, "new-ui-key", null)
        val translation = translationService.find(key, testData.german.self).orElse(null)
        translation.assert.isNotNull()
        translation!!.text.assert.contains("translated with")
        translationId = translation.id
      }
    }
    return translationId!!
  }

  private fun assertAiReviewCreatedAfterFinalizationOf(
    type: BatchJobType,
    languageId: Long,
  ) {
    val events = recorder.events.toList()
    val aiReviewCreated =
      events.indexOfFirst { it.kind == CREATED && it.type == BatchJobType.AI_REVIEW && it.coversLanguage(languageId) }
    val precedingFinalized =
      events.indexOfFirst { it.kind == FINALIZED && it.type == type && it.coversLanguage(languageId) }
    aiReviewCreated.assert.isGreaterThan(-1)
    precedingFinalized.assert.isGreaterThan(-1)
    aiReviewCreated.assert.isGreaterThan(precedingFinalized)
  }

  private fun RecordedEvent.targetItems(): List<BatchTranslationTargetItem> =
    target.map { objectMapper.convertValue(it, BatchTranslationTargetItem::class.java) }

  private fun RecordedEvent.coversLanguage(languageId: Long): Boolean {
    if (type == BatchJobType.AUTO_TRANSLATE) return true
    return targetItems().any { it.languageId == languageId }
  }

  companion object {
    private const val CREATED = "created"
    private const val FINALIZED = "finalized"
  }
}
