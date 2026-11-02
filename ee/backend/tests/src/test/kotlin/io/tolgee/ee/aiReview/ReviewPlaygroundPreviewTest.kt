package io.tolgee.ee.aiReview

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.repository.AiPlaygroundResultRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class ReviewPlaygroundPreviewTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var aiPlaygroundResultRepository: AiPlaygroundResultRepository

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
  fun `a review preview stores the score and issues per key and language`() {
    save()
    startPreview(keyIds = listOf(testData.key.id), languageIds = listOf(testData.german.id)).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val result = results(testData.key.id, testData.german.id)
      result.assert.hasSize(1)
      result[0].score.assert.isEqualTo(100)
      result[0].issues.assert.isEmpty()
      result[0].translation.assert.isNull()
    }
  }

  @Test
  fun `a review preview skips empty translations`() {
    val empty = testData.addKeyWithTranslations("k.empty", deText = "")
    save()
    startPreview(
      keyIds = listOf(testData.key.id, empty.key.id),
      languageIds = listOf(testData.german.id),
    ).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      results(testData.key.id, testData.german.id).assert.hasSize(1)
    }
    Thread.sleep(1000)
    results(empty.key.id, testData.german.id).assert.isEmpty()
  }

  @Test
  fun `a review preview starts an AI_PLAYGROUND_REVIEW job`() {
    save()
    startPreview(keyIds = listOf(testData.key.id), languageIds = listOf(testData.german.id))
      .andIsOk
      .andAssertThatJson { node("type").isEqualTo("AI_PLAYGROUND_REVIEW") }
  }

  @Test
  fun `a review preview without the feature returns 400`() {
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()
    startPreview(keyIds = listOf(testData.key.id), languageIds = listOf(testData.german.id))
      .andIsBadRequest
      .andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  private fun startPreview(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ) = performAuthPost(
    "$projectUrl/start-batch-job/ai-playground-translate",
    mapOf(
      "keyIds" to keyIds,
      "targetLanguageIds" to languageIds,
      "llmPrompt" to
        mapOf(
          "name" to "Review preview",
          "providerName" to "default",
          "type" to "REVIEW",
          "reviewCriteria" to "Use informal German.",
        ),
    ),
  )

  private fun results(
    keyId: Long,
    languageId: Long,
  ) = executeInNewTransaction(platformTransactionManager) {
    aiPlaygroundResultRepository
      .getResults(testData.project.id, testData.user.id, listOf(keyId), listOf(languageId))
      .toList()
      .onEach { it.issues?.size }
  }
}
