package io.tolgee.ee.aiReview

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AiReviewTranslationControllerTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var activityRevisionRepository: ActivityRevisionRepository

  lateinit var rule: AiReviewRuleBuilder

  @BeforeEach
  fun seed() {
    rule =
      testData.addRule {
        name = "UI rule"
        tagIn = mutableListOf("ui")
        tagNotIn = mutableListOf("legal")
        namespaceIn = mutableListOf("")
      }
    seed(testData.uiGerman, AiReviewOutcome.AUTO_REVIEWED, score = 95)
    seed(testData.uiFrench, AiReviewOutcome.AUTO_REVIEWED, score = 95)
    seed(testData.uiCzech, AiReviewOutcome.BELOW_THRESHOLD, score = 70)
    saveTestData()
  }

  private fun seed(
    translation: TranslationBuilder,
    outcome: AiReviewOutcome,
    score: Int,
  ) {
    if (outcome == AiReviewOutcome.AUTO_REVIEWED) {
      translation.self.state = TranslationState.REVIEWED
    }
    translation.addAiReview {
      this.outcome = outcome
      textHash = aiReviewTextHash(translation.self.text)
      this.rule = this@AiReviewTranslationControllerTest.rule.self
      prompt = testData.reviewPrompt.self
      providerName = "gpt-test"
      this.score = score
      threshold = 85
      issues = mutableListOf(AiReviewIssue(AiReviewIssueSeverity.MINOR, "style", "Nit", "Better", 5))
    }
  }

  private val germanId get() = testData.uiGerman.self.id

  private fun url(
    translationId: Long,
    action: String = "",
  ) = "/v2/projects/$projectId/ai-review/translations/$translationId$action"

  @Test
  fun `returns review detail`() {
    performAuthGet(url(germanId)).andIsOk.andAssertThatJson {
      node("translationId").isEqualTo(germanId)
      node("outcome").isEqualTo("AUTO_REVIEWED")
      node("score").isEqualTo(95)
      node("threshold").isEqualTo(85)
      node("dryRun").isEqualTo(false)
      node("ruleName").isEqualTo("UI rule")
      node("ruleScope.tagIn").isEqualTo(listOf("ui"))
      node("ruleScope.tagNotIn").isEqualTo(listOf("legal"))
      node("ruleScope.namespaceIn").isEqualTo(listOf(""))
      node("ruleScope.namespaceNotIn").isEqualTo(emptyList<String>())
      node("ruleScope.languageIds").isEqualTo(emptyList<Long>())
      node("promptName").isEqualTo("Strict review")
      node("providerName").isEqualTo("gpt-test")
      node("issues[0].severity").isEqualTo("MINOR")
      node("issues[0].category").isEqualTo("style")
      node("issues[0].message").isEqualTo("Nit")
      node("issues[0].suggestion").isEqualTo("Better")
      node("issues[0].points").isEqualTo(5)
    }
  }

  @Test
  fun `confirm keeps the translation reviewed and hides the badge`() {
    performAuthPost(url(germanId, "/confirm"), null).andIsOk.andAssertThatJson {
      node("outcome").isEqualTo("CONFIRMED")
    }

    val review = findReview(germanId)!!
    review.outcome.assert.isEqualTo(AiReviewOutcome.CONFIRMED)
    review.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    getTranslation(germanId).state.assert.isEqualTo(TranslationState.REVIEWED)

    performAuthGet("/v2/projects/$projectId/translations?languages=de&filterKeyName=ui-key")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].translations.de.state").isEqualTo("REVIEWED")
        node("_embedded.keys[0].translations.de.aiReview").isNull()
      }
  }

  @Test
  fun `reject sets the translation back to translated as the user`() {
    performAuthPost(url(germanId, "/reject"), null).andIsOk.andAssertThatJson {
      node("state").isEqualTo("TRANSLATED")
    }

    val review = findReview(germanId)!!
    review.outcome.assert.isEqualTo(AiReviewOutcome.REJECTED)
    review.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    getTranslation(germanId).state.assert.isEqualTo(TranslationState.TRANSLATED)

    executeInNewTransaction(platformTransactionManager) {
      val revision =
        activityRevisionRepository
          .findAll()
          .filter { it.projectId == projectId }
          .maxBy { it.id }
      revision.type.assert.isEqualTo(ActivityType.SET_TRANSLATION_STATE)
      revision.authorId.assert.isEqualTo(testData.user.id)
    }
  }

  @Test
  fun `translator without state change permission cannot resolve`() {
    userAccount = testData.translator
    performAuthPost(url(germanId, "/confirm"), null).andIsForbidden
    performAuthPost(url(germanId, "/reject"), null).andIsForbidden
    findReview(germanId)!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
  }

  @Test
  fun `reviewer of another language cannot resolve or view`() {
    userAccount = testData.czechReviewer
    performAuthPost(url(germanId, "/confirm"), null).andIsForbidden
    performAuthPost(url(germanId, "/reject"), null).andIsForbidden
    performAuthGet(url(testData.uiFrench.self.id)).andIsForbidden
    getTranslation(germanId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `cannot resolve a translation that was not auto-reviewed`() {
    performAuthPost(url(testData.uiCzech.self.id, "/confirm"), null)
      .andIsBadRequest
      .andHasErrorMessage(io.tolgee.constants.Message.AI_REVIEW_CANNOT_BE_RESOLVED)
  }

  @Test
  fun `returns 404 for a translation without review`() {
    performAuthGet(url(testData.plainGerman.self.id)).andIsNotFound
  }

  @Test
  fun `returns 404 when the feature flag is off`() {
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    performAuthGet(url(germanId)).andIsNotFound
    performAuthPost(url(germanId, "/confirm"), null).andIsNotFound
    performAuthPost(url(germanId, "/reject"), null).andIsNotFound
    findReview(germanId)!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
  }

  @Test
  fun `requires the plan feature when the flag is on`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_PROMPT_CUSTOMIZATION)
    performAuthGet(url(germanId))
      .andIsBadRequest
      .andHasErrorMessage(io.tolgee.constants.Message.FEATURE_NOT_ENABLED)
  }
}
