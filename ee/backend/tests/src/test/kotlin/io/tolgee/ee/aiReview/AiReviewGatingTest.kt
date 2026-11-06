package io.tolgee.ee.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
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
import org.springframework.context.ApplicationEventPublisher

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewGatingTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var applicationEventPublisher: ApplicationEventPublisher

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

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
  fun `every AI review endpoint returns FEATURE_NOT_ENABLED without the feature`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90)
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()

    val ruleUrl = "$projectUrl/ai-review-rules/${testData.rule.id}"
    val translationUrl = "$projectUrl/translations/${testData.deTranslation.id}/ai-review"
    val ruleBody =
      mapOf("name" to "UI", "mode" to "ACTIVE", "threshold" to 85, "tagIn" to listOf("ui"))
    val runBody = mapOf("ruleId" to testData.rule.id, "languageIds" to emptyList<Long>())

    listOf(
      { performAuthGet("$projectUrl/ai-review-rules") },
      { performAuthPost("$projectUrl/ai-review-rules", ruleBody) },
      { performAuthGet(ruleUrl) },
      { performAuthPut(ruleUrl, ruleBody) },
      { performAuthPut("$ruleUrl/enabled", mapOf("enabled" to false)) },
      { performAuthDelete(ruleUrl) },
      { performAuthGet(translationUrl) },
      { performAuthPut("$translationUrl/confirm", null) },
      { performAuthPut("$translationUrl/reject", null) },
      { performAuthPost("$translationUrl/retry", null) },
      { performAuthPost("$projectUrl/ai-review/run/preview", runBody) },
      { performAuthPost("$projectUrl/ai-review/run", runBody) },
    ).forEachIndexed { index, request ->
      try {
        request().andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
      } catch (e: Throwable) {
        throw AssertionError("Endpoint #$index is not gated: ${e.message}", e)
      }
    }
  }

  @Test
  fun `losing the feature keeps the rows and stops processing`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()

    val job =
      executeInNewTransaction(platformTransactionManager) {
        batchJobService.startJob(
          request = AiReviewRequest(translationIds = listOf(testData.deTranslation.id)),
          project = entityManager.getReference(Project::class.java, testData.project.id),
          author = null,
          type = BatchJobType.AI_REVIEW,
          isHidden = true,
        )
      }
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      batchJobService
        .getJobDtoNoCache(job.id)
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.PENDING)
    row.score.assert.isNull()
  }

  @Test
  fun `regaining the feature reviews the queued rows`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    save()

    executeInNewTransaction(platformTransactionManager) {
      applicationEventPublisher.publishEvent(
        OnOrganizationFeaturesChanged(
          organizationId = testData.project.organizationOwner.id,
          gainedFeatures = setOf(Feature.AI_REVIEW),
          lostFeatures = emptySet(),
        ),
      )
    }

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    }
  }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)
    }
}
