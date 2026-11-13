package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Prompt
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.aiReview.AiReviewRuleRepository
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
class ReviewPromptRevisionTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var aiReviewRuleRepository: AiReviewRuleRepository

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  lateinit var testData: AiReviewTestData
  lateinit var scoreOnlyRunRule: AiReviewRule
  lateinit var otherPrompt: Prompt
  lateinit var otherPromptRule: AiReviewRule

  private val promptUrl get() = "/v2/projects/${testData.project.id}/prompts/${testData.reviewPrompt.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.AI_PROMPT_CUSTOMIZATION)
    testData = AiReviewTestData()
    testData.rule.prompt = testData.reviewPrompt
    scoreOnlyRunRule =
      testData
        .addRule {
          mode = AiReviewRuleMode.SCORE_ONLY
          prompt = testData.reviewPrompt
        }.self
    otherPrompt =
      testData.projectBuilder
        .addPrompt {
          name = "Other review"
          providerName = "default"
          type = PromptType.REVIEW
        }.self
    otherPromptRule =
      testData
        .addRule {
          mode = AiReviewRuleMode.ACTIVE
          prompt = otherPrompt
        }.self
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

  private fun body(
    name: String = "Brand voice review",
    template: String? = null,
    providerName: String = "default",
  ) = mapOf(
    "name" to name,
    "template" to template,
    "providerName" to providerName,
    "type" to "REVIEW",
  )

  @Test
  fun `rename does not change revision and keeps rules Active`() {
    save()
    performAuthPut(promptUrl, body(name = "Renamed")).andIsOk.andAssertThatJson {
      node("revision").isEqualTo(0)
    }
    mode(testData.rule.id).assert.isEqualTo(AiReviewRuleMode.ACTIVE)
  }

  @Test
  fun `template change increases revision and switches its Active rules to Score only`() {
    save()
    performAuthPut(promptUrl, body(template = "{{fragment.reviewJson}}")).andIsOk.andAssertThatJson {
      node("revision").isEqualTo(1)
    }
    mode(testData.rule.id).assert.isEqualTo(AiReviewRuleMode.SCORE_ONLY)
    mode(scoreOnlyRunRule.id).assert.isEqualTo(AiReviewRuleMode.SCORE_ONLY)
    mode(otherPromptRule.id).assert.isEqualTo(AiReviewRuleMode.ACTIVE)
  }

  @Test
  fun `provider change switches rules to Score only`() {
    save()
    performAuthPut(promptUrl, body(providerName = "other-provider")).andIsOk
    mode(testData.rule.id).assert.isEqualTo(AiReviewRuleMode.SCORE_ONLY)
  }

  @Test
  fun `deleting a review prompt sets rules to the default prompt and Active ones to Score only`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90) {
      promptName = testData.reviewPrompt.name
    }
    save()
    executeInNewTransaction(platformTransactionManager) {
      val review = translationAiReviewRepository.findByTranslationId(testData.deTranslation.id)!!
      review.promptId = testData.reviewPrompt.id
      translationAiReviewRepository.save(review)
    }

    performAuthDelete(promptUrl).andIsOk

    executeInNewTransaction(platformTransactionManager) {
      val rule = aiReviewRuleRepository.findById(testData.rule.id).get()
      rule.prompt.assert.isNull()
      rule.mode.assert.isEqualTo(AiReviewRuleMode.SCORE_ONLY)
      aiReviewRuleRepository
        .findById(scoreOnlyRunRule.id)
        .get()
        .prompt.assert
        .isNull()
      val review = translationAiReviewRepository.findByTranslationId(testData.deTranslation.id)!!
      review.promptId.assert.isEqualTo(testData.reviewPrompt.id)
      review.promptName.assert.isEqualTo("Brand voice review")
    }
  }

  @Test
  fun `prompt model returns activeRuleCount`() {
    save()
    performAuthGet(promptUrl).andIsOk.andAssertThatJson {
      node("activeRuleCount").isEqualTo(1)
    }
  }

  private fun mode(ruleId: Long): AiReviewRuleMode =
    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository.findById(ruleId).get().mode
    }
}
