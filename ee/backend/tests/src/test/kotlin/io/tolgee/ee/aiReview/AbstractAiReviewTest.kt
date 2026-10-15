package io.tolgee.ee.aiReview

import com.posthog.server.PostHog
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.development.testDataBuilder.data.AiReviewTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

abstract class AbstractAiReviewTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @MockitoSpyBean
  @Autowired
  lateinit var llmProviderService: LlmProviderService

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  lateinit var testData: AiReviewTestData

  private var saved = false
  private var originalFreeCredits: Long = -1

  open val enabledFeatures: Set<Feature> = setOf(Feature.AI_REVIEW, Feature.AI_PROMPT_CUSTOMIZATION)

  open fun createTestData(): AiReviewTestData = AiReviewTestData()

  @BeforeEach
  fun setupAiReview() {
    testData = createTestData()
    saved = false
    enabledFeaturesProvider.forceEnabled = enabledFeatures
    tolgeeProperties.featureFlags.enabledForAll = listOf(FeatureFlag.AI_REVIEW)
    originalFreeCredits = machineTranslationProperties.freeCreditsAmount
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LlmProperties.LlmProvider(
          type = LlmProviderType.OPENAI,
          tokenPriceInCreditsInput = 2.0,
          tokenPriceInCreditsOutput = 2.0,
          apiUrl = "http://test.com",
        ),
      )
    internalProperties.fakeLlmProviders = true
  }

  @AfterEach
  fun cleanupAiReview() {
    if (saved) {
      waitForProjectJobsCompleted()
      testDataService.cleanTestData(testData.root)
    }
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    machineTranslationProperties.freeCreditsAmount = originalFreeCredits
    llmProperties.enabled = null
    llmProperties.providers = mutableListOf()
    internalProperties.fakeLlmProviders = false
    Mockito.reset(llmProviderService)
  }

  fun saveTestData() {
    testDataService.saveTestData(testData.root)
    saved = true
    userAccount = testData.user
  }

  val projectId: Long get() = testData.project.id

  fun setText(
    keyName: String,
    languageTag: String,
    text: String,
    namespace: String? = null,
  ) {
    performAuthPut(
      "/v2/projects/$projectId/translations",
      SetTranslationsWithKeyDto(key = keyName, namespace = namespace, translations = mapOf(languageTag to text)),
    ).andIsOk
  }

  fun findReview(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)?.also {
        it.rule?.name
        it.resolvedBy?.id
      }
    }

  fun getTranslation(translationId: Long): Translation =
    executeInNewTransaction(platformTransactionManager) {
      translationService.get(translationId)
    }

  fun waitForReview(
    translationId: Long,
    assertion: (TranslationAiReview?) -> Unit,
  ) {
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      assertion(findReview(translationId))
    }
  }

  fun aiReviewJobs(): List<BatchJob> = jobsOfType(BatchJobType.AI_REVIEW)

  fun jobsOfType(type: BatchJobType): List<BatchJob> =
    executeInNewTransaction(platformTransactionManager) {
      batchJobService.getAllByProjectId(projectId).filter { it.type == type }
    }

  fun waitForProjectJobsCompleted() {
    waitFor(timeout = 30_000, pollTime = 100) {
      executeInNewTransaction(platformTransactionManager) {
        batchJobService.getAllByProjectId(projectId).all { it.status.completed }
      }
    }
  }
}
