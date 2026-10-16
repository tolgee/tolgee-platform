package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.dtos.request.key.UpdateNamespaceDto
import io.tolgee.ee.data.aiReview.AiReviewRuleRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.model.aiReview.AiReviewLanguageThreshold
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AiReviewRuleControllerTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var aiReviewRuleRepository: AiReviewRuleRepository

  private val url get() = "/v2/projects/$projectId/ai-review/rules"

  private fun validRequest() =
    AiReviewRuleRequest(
      name = "  UI strings  ",
      mode = AiReviewRuleMode.ACTIVE,
      threshold = 90,
      languageThresholds =
        listOf(
          AiReviewLanguageThreshold(languageId = testData.german.self.id, never = true),
          AiReviewLanguageThreshold(languageId = testData.czech.self.id, threshold = 70),
        ),
      promptId = testData.reviewPrompt.self.id,
      skipWithOpenQaIssues = false,
      tagIn = listOf("ui", ""),
      tagNotIn = listOf("legal"),
      namespaceIn = listOf(""),
      namespaceNotIn = listOf("checkout"),
      languageIds = listOf(testData.german.self.id, testData.czech.self.id),
      branchIn = listOf(" feature/* ", "main", "main"),
      branchNotIn = listOf("feature/legacy-*", " "),
    )

  @Test
  fun `creates a rule`() {
    saveTestData()
    performAuthPost(url, validRequest()).andIsOk.andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo("UI strings")
      node("enabled").isEqualTo(true)
      node("mode").isEqualTo("ACTIVE")
      node("threshold").isEqualTo(90)
      node("languageThresholds[0].languageId").isEqualTo(testData.german.self.id)
      node("languageThresholds[0].never").isEqualTo(true)
      node("languageThresholds[1].threshold").isEqualTo(70)
      node("promptId").isEqualTo(testData.reviewPrompt.self.id)
      node("promptName").isEqualTo("Strict review")
      node("skipWithOpenQaIssues").isEqualTo(false)
      node("tagIn").isEqualTo(listOf("ui", ""))
      node("tagNotIn").isEqualTo(listOf("legal"))
      node("namespaceIn").isEqualTo(listOf(""))
      node("namespaceNotIn").isEqualTo(listOf("checkout"))
      node("languageIds").isEqualTo(listOf(testData.german.self.id, testData.czech.self.id))
      node("branchIn").isEqualTo(listOf("feature/*", "main"))
      node("branchNotIn").isEqualTo(listOf("feature/legacy-*"))
      node("stats.scored").isEqualTo(0)
    }
  }

  @Test
  fun `creates a rule with defaults and the default prompt`() {
    saveTestData()
    performAuthPost(url, AiReviewRuleRequest(name = " ")).andIsOk.andAssertThatJson {
      node("name").isNull()
      node("mode").isEqualTo("DRY_RUN")
      node("threshold").isEqualTo(85)
      node("promptId").isNull()
      node("skipWithOpenQaIssues").isEqualTo(true)
    }
  }

  @Test
  fun `lists, updates and deletes rules`() {
    val rule = testData.addRule { name = "Existing" }
    saveTestData()

    performAuthGet(url).andIsOk.andAssertThatJson {
      node("_embedded.rules").isArray.hasSize(1)
      node("_embedded.rules[0].name").isEqualTo("Existing")
    }

    performAuthPut("$url/${rule.self.id}", validRequest().copy(name = "Renamed", enabled = false))
      .andIsOk
      .andAssertThatJson {
        node("name").isEqualTo("Renamed")
        node("enabled").isEqualTo(false)
        node("threshold").isEqualTo(90)
      }

    performAuthDelete("$url/${rule.self.id}").andIsOk
    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository.findAllByProjectId(projectId).assert.isEmpty()
    }
  }

  @Test
  fun `returns 404 for unknown rule`() {
    saveTestData()
    performAuthPut("$url/999999999", validRequest())
      .andIsNotFound
      .andHasErrorMessage(Message.AI_REVIEW_RULE_NOT_FOUND)
  }

  @Test
  fun `validates the request`() {
    saveTestData()
    performAuthPost(url, validRequest().copy(threshold = 101)).andIsBadRequest
    performAuthPost(url, validRequest().copy(threshold = -1)).andIsBadRequest
    performAuthPost(url, validRequest().copy(promptId = testData.translatePrompt.self.id))
      .andIsBadRequest
      .andHasErrorMessage(Message.PROMPT_TYPE_NOT_SUPPORTED)
    performAuthPost(url, validRequest().copy(promptId = 999999999)).andIsNotFound
    performAuthPost(url, validRequest().copy(languageIds = listOf(999999999)))
      .andIsBadRequest
      .andHasErrorMessage(Message.LANGUAGE_NOT_FROM_PROJECT)
    performAuthPost(
      url,
      validRequest().copy(
        languageThresholds = listOf(AiReviewLanguageThreshold(testData.german.self.id, threshold = 80, never = true)),
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.AI_REVIEW_INVALID_THRESHOLD)
    performAuthPost(
      url,
      validRequest().copy(languageThresholds = listOf(AiReviewLanguageThreshold(testData.german.self.id))),
    ).andIsBadRequest.andHasErrorMessage(Message.AI_REVIEW_INVALID_THRESHOLD)
    performAuthPost(
      url,
      validRequest().copy(
        languageThresholds = listOf(AiReviewLanguageThreshold(testData.german.self.id, threshold = 120)),
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.AI_REVIEW_INVALID_THRESHOLD)
    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository.findAllByProjectId(projectId).assert.isEmpty()
    }
  }

  @Test
  fun `prompt viewer can read but not write`() {
    val rule = testData.addRule()
    saveTestData()
    userAccount = testData.promptViewer
    performAuthGet(url).andIsOk
    performAuthPost(url, validRequest()).andIsForbidden
    performAuthPut("$url/${rule.self.id}", validRequest()).andIsForbidden
    performAuthDelete("$url/${rule.self.id}").andIsForbidden
  }

  @Test
  fun `translator cannot access rules`() {
    saveTestData()
    userAccount = testData.translator
    performAuthGet(url).andIsForbidden
    performAuthPost(url, validRequest()).andIsForbidden
  }

  @Test
  fun `returns 404 on every endpoint when the feature flag is off`() {
    val rule = testData.addRule()
    saveTestData()
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    performAuthGet(url).andIsNotFound
    performAuthPost(url, validRequest()).andIsNotFound
    performAuthPut("$url/${rule.self.id}", validRequest()).andIsNotFound
    performAuthDelete("$url/${rule.self.id}").andIsNotFound
  }

  @Test
  fun `requires the plan feature when the flag is on`() {
    saveTestData()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_PROMPT_CUSTOMIZATION)
    performAuthGet(url).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
    performAuthPost(url, validRequest()).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  fun `returns stats per rule`() {
    val dryRunRule = testData.addRule { mode = AiReviewRuleMode.DRY_RUN }
    val activeRule = testData.addRule()
    seedRow(testData.uiGerman, dryRunRule, AiReviewOutcome.WOULD_AUTO_REVIEW, dryRun = true)
    seedRow(testData.uiCzech, dryRunRule, AiReviewOutcome.BELOW_THRESHOLD, dryRun = true)
    seedRow(testData.uiFrench, dryRunRule, AiReviewOutcome.WOULD_AUTO_REVIEW, dryRun = true)
    seedRow(testData.legalGerman, activeRule, AiReviewOutcome.AUTO_REVIEWED, dryRun = false)
    seedRow(testData.checkoutGerman, activeRule, AiReviewOutcome.REJECTED, dryRun = false)
    seedRow(testData.plainGerman, activeRule, AiReviewOutcome.CONFIRMED, dryRun = false)
    saveTestData()

    performAuthGet(url).andIsOk.andAssertThatJson {
      node("_embedded.rules[0].id").isEqualTo(dryRunRule.self.id)
      node("_embedded.rules[0].stats.scored").isEqualTo(3)
      node("_embedded.rules[0].stats.wouldAutoReview").isEqualTo(2)
      node("_embedded.rules[0].stats.autoReviewed").isEqualTo(0)
      node("_embedded.rules[1].id").isEqualTo(activeRule.self.id)
      node("_embedded.rules[1].stats.autoReviewed").isEqualTo(3)
      node("_embedded.rules[1].stats.rejected").isEqualTo(1)
      node("_embedded.rules[1].stats.scored").isEqualTo(0)
    }
  }

  @Test
  fun `deleting the rule and prompt keeps the results they produced`() {
    val rule = testData.addRule { prompt = testData.reviewPrompt.self }
    testData.uiGerman.self.state = TranslationState.REVIEWED
    seedRow(testData.uiGerman, rule, AiReviewOutcome.AUTO_REVIEWED, dryRun = false)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthDelete("$url/${rule.self.id}").andIsOk
    performAuthDelete("/v2/projects/$projectId/prompts/${testData.reviewPrompt.self.id}").andIsOk

    executeInNewTransaction(platformTransactionManager) {
      val review = translationAiReviewRepository.findByTranslationId(translationId)!!
      review.rule.assert.isNull()
      review.prompt.assert.isNull()
      review.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    performAuthGet("/v2/projects/$projectId/ai-review/translations/$translationId").andIsOk.andAssertThatJson {
      node("outcome").isEqualTo("AUTO_REVIEWED")
    }
    performAuthGet("/v2/projects/$projectId/translations?languages=de").andIsOk
    performAuthGet(url).andIsOk
  }

  @Test
  fun `deleting a language removes it from rules and disables rules scoped only to it`() {
    val czechOnly = testData.addRule { name = "Czech only" }.scopeLanguages(testData.czech.self)
    val czechAndGerman =
      testData
        .addRule { name = "Czech and German" }
        .scopeLanguages(testData.czech.self, testData.german.self)
        .languageThreshold(testData.czech.self, 70)
    saveTestData()

    performAuthDelete("/v2/projects/$projectId/languages/${testData.czech.self.id}").andIsOk

    executeInNewTransaction(platformTransactionManager) {
      val first = aiReviewRuleRepository.findById(czechOnly.self.id).get()
      first.languageIds.assert.isEmpty()
      first.enabled.assert.isFalse()
      val second = aiReviewRuleRepository.findById(czechAndGerman.self.id).get()
      second.languageIds.assert.containsExactly(testData.german.self.id)
      second.languageThresholds.assert.isEmpty()
      second.enabled.assert.isTrue()
    }
  }

  @Test
  fun `deleting a language drops its override from a rule that applies to all languages`() {
    val allLanguages = testData.addRule { name = "All languages" }.languageThreshold(testData.czech.self, 70)
    saveTestData()

    performAuthDelete("/v2/projects/$projectId/languages/${testData.czech.self.id}").andIsOk

    executeInNewTransaction(platformTransactionManager) {
      val rule = aiReviewRuleRepository.findById(allLanguages.self.id).get()
      rule.languageThresholds.assert.isEmpty()
      rule.languageIds.assert.isEmpty()
      rule.enabled.assert.isTrue()
    }
  }

  @Test
  fun `renaming a namespace renames it in rule scopes`() {
    val excluding = testData.addRule { namespaceNotIn = mutableListOf("checkout") }
    val including = testData.addRule { namespaceIn = mutableListOf("checkout", "other") }
    saveTestData()
    val namespaceId =
      testData.checkoutKey.self.namespace!!
        .id

    performAuthPut("/v2/projects/$projectId/namespaces/$namespaceId", UpdateNamespaceDto("checkout-flow")).andIsOk

    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository
        .findById(excluding.self.id)
        .get()
        .namespaceNotIn.assert
        .containsExactly("checkout-flow")
      aiReviewRuleRepository
        .findById(including.self.id)
        .get()
        .namespaceIn.assert
        .containsExactly("checkout-flow", "other")
    }
  }

  @Test
  fun `raising the threshold demotes translations the rule reviewed below the new threshold`() {
    val rule = testData.addRule { name = "UI" }
    testData.uiGerman.self.state = TranslationState.REVIEWED
    seedRow(testData.uiGerman, rule, AiReviewOutcome.AUTO_REVIEWED, dryRun = false)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPut(
      "$url/${rule.self.id}",
      AiReviewRuleRequest(name = "UI", mode = AiReviewRuleMode.ACTIVE, threshold = 95),
    ).andIsOk

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    findReview(translationId)!!.threshold.assert.isEqualTo(95)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `a new rule with a never override demotes translations it covers`() {
    val rule = testData.addRule { name = "UI" }
    testData.uiGerman.self.state = TranslationState.REVIEWED
    seedRow(testData.uiGerman, rule, AiReviewOutcome.AUTO_REVIEWED, dryRun = false)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPost(
      url,
      AiReviewRuleRequest(
        name = "No German",
        mode = AiReviewRuleMode.ACTIVE,
        languageThresholds = listOf(AiReviewLanguageThreshold(languageId = testData.german.self.id, never = true)),
      ),
    ).andIsOk

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `lowering the threshold keeps earlier reviews`() {
    val rule = testData.addRule { name = "UI" }
    testData.uiGerman.self.state = TranslationState.REVIEWED
    seedRow(testData.uiGerman, rule, AiReviewOutcome.AUTO_REVIEWED, dryRun = false)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPut(
      "$url/${rule.self.id}",
      AiReviewRuleRequest(name = "UI", mode = AiReviewRuleMode.ACTIVE, threshold = 60),
    ).andIsOk

    waitForProjectJobsCompleted()
    findReview(translationId)!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `narrowing the scope hands translations to a stricter rule and demotes them`() {
    val rule = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule {
      tagIn = mutableListOf("ui")
      threshold = 95
    }
    testData.uiGerman.self.state = TranslationState.REVIEWED
    seedRow(testData.uiGerman, rule, AiReviewOutcome.AUTO_REVIEWED, dryRun = false)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPut(
      "$url/${rule.self.id}",
      AiReviewRuleRequest(mode = AiReviewRuleMode.ACTIVE, threshold = 85, tagIn = listOf("legal")),
    ).andIsOk

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    findReview(translationId)!!.threshold.assert.isEqualTo(95)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `a new dry run rule with a higher threshold keeps active reviews`() {
    val rule = testData.addRule()
    testData.uiGerman.self.state = TranslationState.REVIEWED
    seedRow(testData.uiGerman, rule, AiReviewOutcome.AUTO_REVIEWED, dryRun = false)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPost(url, AiReviewRuleRequest(mode = AiReviewRuleMode.DRY_RUN, threshold = 95)).andIsOk

    waitForProjectJobsCompleted()
    findReview(translationId)!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `a new dry run rule with a never override keeps active reviews`() {
    val rule = testData.addRule()
    testData.uiGerman.self.state = TranslationState.REVIEWED
    seedRow(testData.uiGerman, rule, AiReviewOutcome.AUTO_REVIEWED, dryRun = false)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPost(
      url,
      AiReviewRuleRequest(
        mode = AiReviewRuleMode.DRY_RUN,
        languageThresholds = listOf(AiReviewLanguageThreshold(languageId = testData.german.self.id, never = true)),
      ),
    ).andIsOk

    waitForProjectJobsCompleted()
    findReview(translationId)!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `a new dry run rule with a higher threshold demotes dry run results`() {
    val rule = testData.addRule { mode = AiReviewRuleMode.DRY_RUN }
    seedRow(testData.uiGerman, rule, AiReviewOutcome.WOULD_AUTO_REVIEW, dryRun = true)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPost(url, AiReviewRuleRequest(mode = AiReviewRuleMode.DRY_RUN, threshold = 95)).andIsOk

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    findReview(translationId)!!.threshold.assert.isEqualTo(95)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `retries failed reviews of the rule`() {
    val rule = testData.addRule { tagIn = mutableListOf("ui") }
    seedRow(testData.uiGerman, rule, AiReviewOutcome.FAILED, dryRun = false)
    seedRow(testData.legalGerman, rule, AiReviewOutcome.FAILED, dryRun = false)
    testData.uiCzech.self.state = TranslationState.REVIEWED
    seedRow(testData.uiCzech, rule, AiReviewOutcome.FAILED, dryRun = false)
    saveTestData()
    val retriedId = testData.uiGerman.self.id
    val unmatchedId = testData.legalGerman.self.id
    val reviewedId = testData.uiCzech.self.id

    performAuthGet(url).andIsOk.andAssertThatJson {
      node("_embedded.rules[0].stats.failed").isEqualTo(2)
    }

    performAuthPost("$url/${rule.self.id}/retry-failed", null).andIsOk.andAssertThatJson {
      node("queued").isEqualTo(1)
    }

    waitForReview(retriedId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    getTranslation(retriedId).state.assert.isEqualTo(TranslationState.REVIEWED)
    findReview(unmatchedId).assert.isNull()
    findReview(reviewedId)!!.outcome.assert.isEqualTo(AiReviewOutcome.FAILED)
    performAuthGet(url).andIsOk.andAssertThatJson {
      node("_embedded.rules[0].stats.failed").isEqualTo(0)
    }
  }

  @Test
  fun `retrying failed reviews requires prompt edit permission, the flag and the plan feature`() {
    val rule = testData.addRule()
    saveTestData()
    val retryUrl = "$url/${rule.self.id}/retry-failed"

    performAuthPost(retryUrl, null).andIsOk.andAssertThatJson { node("queued").isEqualTo(0) }
    performAuthPost("$url/999999999/retry-failed", null)
      .andIsNotFound
      .andHasErrorMessage(Message.AI_REVIEW_RULE_NOT_FOUND)

    userAccount = testData.promptViewer
    performAuthPost(retryUrl, null).andIsForbidden

    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_PROMPT_CUSTOMIZATION)
    performAuthPost(retryUrl, null).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)

    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    performAuthPost(retryUrl, null).andIsNotFound
  }

  private fun seedRow(
    translation: TranslationBuilder,
    rule: AiReviewRuleBuilder,
    outcome: AiReviewOutcome,
    dryRun: Boolean,
  ) {
    translation.addAiReview {
      this.outcome = outcome
      this.rule = rule.self
      this.dryRun = dryRun
      textHash = aiReviewTextHash(translation.self.text)
      score = 90
      threshold = 85
    }
  }
}
