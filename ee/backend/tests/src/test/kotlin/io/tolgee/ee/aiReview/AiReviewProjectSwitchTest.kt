package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewRuleService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
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

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewProjectSwitchTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var aiReviewRuleService: AiReviewRuleService

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `rules cannot be changed while the project switch is off`() {
    testData.project.useAiReview = false
    save()
    val ruleUrl = "$projectUrl/ai-review-rules/${testData.rule.id}"

    performAuthPost("$projectUrl/ai-review-rules", mapOf("name" to "New", "tagIn" to listOf("web")))
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_DISABLED)
    performAuthPut(ruleUrl, mapOf("name" to "Renamed"))
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_DISABLED)
    performAuthPut("$ruleUrl/enabled", mapOf("enabled" to false))
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_DISABLED)
    performAuthDelete(ruleUrl)
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_DISABLED)

    performAuthPut("$projectUrl/ai-review-rules/enabled", mapOf("enabled" to true)).andIsOk
    performAuthPut(ruleUrl, mapOf("name" to "Renamed")).andIsOk
  }

  @Test
  fun `enabling creates the default rule when the project has none`() {
    val data = BaseTestData()
    testDataService.saveTestData(data.root)
    try {
      userAccount = data.user
      val projectId = data.projectBuilder.self.id

      performAuthPut("/v2/projects/$projectId/ai-review-rules/enabled", mapOf("enabled" to true)).andIsOk

      executeInNewTransaction(platformTransactionManager) {
        entityManager
          .find(Project::class.java, projectId)
          .useAiReview.assert
          .isTrue()
        val rules = aiReviewRuleService.list(projectId)
        rules.assert.hasSize(1)
        val rule = rules[0]
        rule.enabled.assert.isTrue()
        rule.mode.assert.isEqualTo(AiReviewRuleMode.SCORE_ONLY)
        rule.threshold.assert.isEqualTo(85)
        rule.never.assert.isFalse()
        rule.name.assert.isEqualTo("Default rule")
        rule.prompt.assert.isNull()
        rule.skipOpenQaIssues.assert.isTrue()
        rule.tagIn.assert.isEmpty()
        rule.tagNotIn.assert.isEmpty()
        rule.namespaceIn.assert.isEmpty()
        rule.namespaceNotIn.assert.isEmpty()
        rule.branchIn.assert.isEmpty()
        rule.branchNotIn.assert.isEmpty()
        rule.languages.assert.isEmpty()
      }
    } finally {
      testDataService.cleanTestData(data.root)
    }
  }

  @Test
  fun `enabling twice creates only one rule`() {
    val data = BaseTestData()
    testDataService.saveTestData(data.root)
    try {
      userAccount = data.user
      val projectId = data.projectBuilder.self.id

      performAuthPut("/v2/projects/$projectId/ai-review-rules/enabled", mapOf("enabled" to true)).andIsOk
      performAuthPut("/v2/projects/$projectId/ai-review-rules/enabled", mapOf("enabled" to true)).andIsOk

      executeInNewTransaction(platformTransactionManager) {
        aiReviewRuleService.list(projectId).assert.hasSize(1)
      }
    } finally {
      testDataService.cleanTestData(data.root)
    }
  }

  @Test
  fun `enabling creates no rule when the project already has one`() {
    testData.project.useAiReview = false
    save()

    performAuthPut("$projectUrl/ai-review-rules/enabled", mapOf("enabled" to true)).andIsOk

    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .find(Project::class.java, testData.project.id)
        .useAiReview.assert
        .isTrue()
      aiReviewRuleService.list(testData.project.id).assert.hasSize(1)
    }
  }

  @Test
  fun `disabling drops pending queue rows and keeps results`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.OVERRIDDEN, 95) {
      reReviewRequested = true
    }
    val approved = testData.addKeyWithTranslations("keep.key").de
    testData.addReview(approved, AiReviewStatus.APPROVED, 90)
    save()

    performAuthPut("$projectUrl/ai-review-rules/enabled", mapOf("enabled" to false)).andIsOk

    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .find(Project::class.java, testData.project.id)
        .useAiReview.assert
        .isFalse()
    }
    review(testData.deTranslation.id).assert.isNull()
    val confirmed = review(testData.frTranslation.id)!!
    confirmed.status.assert.isEqualTo(AiReviewStatus.OVERRIDDEN)
    confirmed.reReviewRequested.assert.isFalse()
    review(approved.self.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
  }

  @Test
  fun `the toggle is logged with its own activity type`() {
    save()

    performAuthPut("$projectUrl/ai-review-rules/enabled", mapOf("enabled" to false)).andIsOk

    performAuthGet("$projectUrl/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].type").isEqualTo("AI_REVIEW_ENABLED_UPDATE")
      node("_embedded.activities[0].modifiedEntities.Project[0].modifications.useAiReview.old").isEqualTo(true)
      node("_embedded.activities[0].modifiedEntities.Project[0].modifications.useAiReview.new").isEqualTo(false)
    }
  }

  @Test
  fun `a user with prompts view only cannot toggle`() {
    save()
    userAccount = testData.promptsViewer

    performAuthPut("$projectUrl/ai-review-rules/enabled", mapOf("enabled" to false)).andIsForbidden
  }

  @Test
  fun `toggling without the org feature is rejected`() {
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()

    performAuthPut("$projectUrl/ai-review-rules/enabled", mapOf("enabled" to false))
      .andIsBadRequest
      .andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  fun `the project model carries the switch state`() {
    testData.project.useAiReview = false
    save()

    performAuthGet(projectUrl).andIsOk.andAssertThatJson {
      node("useAiReview").isEqualTo(false)
    }
    performAuthPut("$projectUrl/ai-review-rules/enabled", mapOf("enabled" to true)).andIsOk
    performAuthGet(projectUrl).andIsOk.andAssertThatJson {
      node("useAiReview").isEqualTo(true)
    }
  }

  @Test
  fun `with the switch off, review detail and actions are rejected`() {
    testData.project.useAiReview = false
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90)
    save()

    val translationUrl = "$projectUrl/translations/${testData.deTranslation.id}/ai-review"
    val runBody = mapOf("ruleId" to testData.rule.id, "languageIds" to emptyList<Long>())
    listOf(
      { performAuthGet(translationUrl) },
      { performAuthPost("$translationUrl/retry", null) },
      { performAuthPost("$projectUrl/ai-review/run/preview", runBody) },
      { performAuthPost("$projectUrl/ai-review/run", runBody) },
    ).forEachIndexed { index, request ->
      try {
        request().andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED_FOR_PROJECT)
      } catch (e: Throwable) {
        throw AssertionError("Endpoint #$index is not gated by the project switch: ${e.message}", e)
      }
    }
  }

  @Test
  fun `with the switch off, playground review runs are rejected`() {
    testData.project.useAiReview = false
    save()

    performAuthPost(
      "$projectUrl/start-batch-job/ai-playground-translate",
      mapOf(
        "keyIds" to listOf(testData.key.id),
        "targetLanguageIds" to listOf(testData.german.id),
        "llmPrompt" to
          mapOf(
            "name" to "Review preview",
            "providerName" to "default",
            "type" to "REVIEW",
          ),
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED_FOR_PROJECT)

    performAuthPost(
      "$projectUrl/prompts/run",
      mapOf(
        "template" to null,
        "keyId" to testData.key.id,
        "targetLanguageId" to testData.german.id,
        "provider" to "default",
        "basicPromptOptions" to null,
        "type" to "REVIEW",
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED_FOR_PROJECT)
  }

  @Test
  fun `with the switch off, rules and review prompts stay readable`() {
    testData.project.useAiReview = false
    save()

    performAuthGet("$projectUrl/ai-review-rules").andIsOk
    performAuthGet("$projectUrl/prompts?type=REVIEW").andIsOk
  }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)
    }
}
