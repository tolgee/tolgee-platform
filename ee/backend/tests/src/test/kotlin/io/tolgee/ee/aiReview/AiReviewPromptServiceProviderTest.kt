package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.llm.OpenaiApiService
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewPromptService
import io.tolgee.ee.service.aiReview.AiReviewResponseSchema
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class AiReviewPromptServiceProviderTest : AbstractSpringTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var aiReviewPromptService: AiReviewPromptService

  @Autowired
  @MockitoBean
  private lateinit var openaiApiService: OpenaiApiService

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
    testData = AiReviewTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = null
    llmProperties.enabled = false
    llmProperties.providers = mutableListOf()
  }

  @Test
  fun `runReview passes the response schema in LlmParams`() {
    stubResponse(RESPONSE)
    runReview()
    val captor = argumentCaptor<LlmParams>()
    verify(openaiApiService).translate(captor.capture(), any(), any())
    captor.firstValue.responseSchema.assert
      .isEqualTo(AiReviewResponseSchema.SCHEMA)
    captor.firstValue.shouldOutputJson.assert
      .isTrue()
  }

  private fun stubResponse(response: String) {
    whenever(openaiApiService.defaultAttempts()).thenReturn(listOf(30))
    whenever(
      openaiApiService.translate(any(), any(), any()),
    ).thenReturn(PromptResult(response = response, usage = null))
  }

  private fun runReview() =
    executeInNewTransaction(platformTransactionManager) {
      aiReviewPromptService.runReview(
        organizationId = testData.project.organizationOwner.id,
        projectId = testData.project.id,
        translation = translationService.get(testData.deTranslation.id),
        promptId = null,
        priority = LlmProviderPriority.LOW,
      )
    }

  companion object {
    private const val RESPONSE =
      """{"issues": [
        {"severity": "major", "category": "accuracy", "message": "Wrong verb.", "points": 35},
        {"severity": "minor", "category": null, "message": "Missing period.", "points": 10}
      ]}"""
  }
}
