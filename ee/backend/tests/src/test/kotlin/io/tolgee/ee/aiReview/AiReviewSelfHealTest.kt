package io.tolgee.ee.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.aiReview.AiReviewActivityListener
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import java.security.MessageDigest

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewSelfHealTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var aiReviewActivityListener: AiReviewActivityListener

  lateinit var testData: AiReviewTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LlmProperties.LlmProvider(
          name = "server-provider",
          type = LlmProviderType.OPENAI,
          apiUrl = "http://test.com",
          model = "gpt-test",
        ),
      )
    internalProperties.fakeLlmProviders = true
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
    llmProperties.enabled = false
    llmProperties.providers = mutableListOf()
    internalProperties.fakeLlmProviders = false
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `the claim of a failed job is released and the row is reviewed`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    val deadJob = addJob(BatchJobStatus.FAILED)
    save()
    setBatchJobIdOnRow(testData.deTranslation.id, deadJob.id)

    fireFinalized(aiReviewJob(handlingStuckItems = false))

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    }
  }

  @Test
  fun `a stuck PENDING row with fresh QA is picked up after any QA job finishes`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.QA_CHECKS)
    testData.project.useQaChecks = true
    testData.deTranslation.qaChecksStale = false
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    val other = testData.addKeyWithTranslations("other.key")
    save()

    startQaJob(other.key.id, testData.french.id)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    }
  }

  @Test
  fun `a queued row with a stale text hash is reviewed for the current text`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    save()
    setTextHashOnRow(testData.deTranslation.id, "stale-hash")

    fireFinalized(aiReviewJob(handlingStuckItems = false))

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val row = review(testData.deTranslation.id)!!
      row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      row.textHash.assert.isEqualTo(sha256Hex("Jetzt bezahlen"))
    }
  }

  @Test
  fun `self-heal does not start when an AI review job is active`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    addJob(BatchJobStatus.RUNNING)
    save()
    val baseline = aiReviewJobCount()

    fireFinalized(aiReviewJob(handlingStuckItems = false))

    aiReviewJobCount().assert.isEqualTo(baseline)
  }

  @Test
  fun `a self-heal job does not trigger another self-heal`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    save()
    val baseline = aiReviewJobCount()

    fireFinalized(aiReviewJob(handlingStuckItems = true))

    aiReviewJobCount().assert.isEqualTo(baseline)
  }

  private fun aiReviewJob(handlingStuckItems: Boolean): BatchJobDto =
    BatchJobDto(
      id = -1,
      projectId = testData.project.id,
      authorId = null,
      target = listOf(),
      totalItems = 0,
      totalChunks = 0,
      chunkSize = 5,
      status = BatchJobStatus.SUCCESS,
      type = BatchJobType.AI_REVIEW,
      params = mapOf("handlingStuckItems" to handlingStuckItems),
      maxPerJobConcurrency = -1,
      jobCharacter = io.tolgee.batch.JobCharacter.SLOW,
      hidden = true,
      debouncingKey = null,
    )

  private fun fireFinalized(job: BatchJobDto) {
    aiReviewActivityListener.onBatchJobFinalized(OnBatchJobFinalized(job, activityRevisionId = null))
  }

  private fun addJob(status: BatchJobStatus): BatchJob =
    testData.projectBuilder
      .addBatchJob {
        type = BatchJobType.AI_REVIEW
        this.status = status
        hidden = true
      }.self

  private fun setBatchJobIdOnRow(
    translationId: Long,
    jobId: Long,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      val row = translationAiReviewRepository.findByTranslationId(translationId)!!
      row.batchJobId = jobId
      translationAiReviewRepository.save(row)
    }
  }

  private fun setTextHashOnRow(
    translationId: Long,
    textHash: String,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      val row = translationAiReviewRepository.findByTranslationId(translationId)!!
      row.textHash = textHash
      translationAiReviewRepository.save(row)
    }
  }

  private fun startQaJob(
    keyId: Long,
    languageId: Long,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      val projectRef = entityManager.getReference(Project::class.java, testData.project.id)
      batchJobService.startJob(
        request =
          QaCheckRequest(
            target = listOf(BatchTranslationTargetItem(keyId = keyId, languageId = languageId)),
          ),
        project = projectRef,
        author = null,
        type = BatchJobType.QA_CHECK,
        isHidden = true,
      )
    }
  }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)
    }

  private fun aiReviewJobCount(): Long =
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createQuery(
          "select count(j) from BatchJob j where j.project.id = :projectId and j.type = :type",
          java.lang.Long::class.java,
        ).setParameter("projectId", testData.project.id)
        .setParameter("type", BatchJobType.AI_REVIEW)
        .singleResult
        .toLong()
    }

  private fun sha256Hex(text: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(text.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
}
