package io.tolgee.ee.aiReview

import io.tolgee.development.testDataBuilder.data.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewRuleMatcher.KeyScope
import io.tolgee.ee.service.aiReview.AiReviewRunClassifier
import io.tolgee.ee.service.aiReview.AiReviewRunClassifier.Candidate
import io.tolgee.ee.service.aiReview.AiReviewRunClassifier.Category
import io.tolgee.ee.service.aiReview.AiReviewRunClassifier.Context
import io.tolgee.ee.service.aiReview.AiReviewRunClassifier.Kind
import io.tolgee.model.aiReview.AiReviewLanguageThreshold
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class AiReviewRunClassifierTest {
  private val testData = AiReviewTestData()
  private var nextId = 1L

  private fun rule(ft: AiReviewRule.() -> Unit = {}): AiReviewRule =
    testData
      .addRule {
        id = nextId++
        ft()
      }.self

  private val ui = KeyScope(tags = setOf("ui"), namespace = null)
  private val legal = KeyScope(tags = setOf("legal"), namespace = null)

  private fun candidate(
    category: Category = Category.NO_RESULT,
    scope: KeyScope = ui,
    languageId: Long = LANGUAGE,
    hasOpenQaIssues: Boolean = false,
  ) = Candidate(category, hasOpenQaIssues, scope, languageId)

  private fun context(
    vararg rules: AiReviewRule,
    rereview: Boolean = false,
    qaCovered: Boolean = false,
    scopeRule: AiReviewRule? = null,
  ) = Context(
    rules = rules.toList(),
    qaCoveredLanguageIds = setOfNotNull(LANGUAGE.takeIf { qaCovered }),
    rereview = rereview,
    scopeRule = scopeRule,
  )

  @Test
  fun `reviews translations without a result and with a failed one`() {
    val rule = rule { tagIn = mutableListOf("ui") }
    val decision = AiReviewRunClassifier.classify(candidate(), context(rule))
    decision.kind.assert.isEqualTo(Kind.REVIEW)
    decision.rule.assert.isEqualTo(rule)
    AiReviewRunClassifier
      .classify(candidate(Category.PENDING), context(rule))
      .kind.assert
      .isEqualTo(Kind.REVIEW)
    AiReviewRunClassifier
      .classify(candidate(Category.FAILED), context(rule))
      .kind.assert
      .isEqualTo(Kind.REVIEW)
  }

  @Test
  fun `never touches human reviewed translations, even with re-review`() {
    val rule = rule()
    AiReviewRunClassifier
      .classify(candidate(Category.HUMAN_REVIEWED), context(rule, rereview = true))
      .kind.assert
      .isEqualTo(Kind.HUMAN_REVIEWED)
  }

  @Test
  fun `skips keys no rule matches and languages set to never`() {
    val rule =
      rule {
        tagIn = mutableListOf("ui")
        languageThresholds = mutableListOf(AiReviewLanguageThreshold(languageId = OTHER_LANGUAGE, never = true))
      }
    AiReviewRunClassifier
      .classify(candidate(scope = legal), context(rule))
      .kind.assert
      .isEqualTo(Kind.NO_RULE)
    AiReviewRunClassifier
      .classify(candidate(languageId = OTHER_LANGUAGE), context(rule))
      .kind.assert
      .isEqualTo(Kind.NEVER)
  }

  @Test
  fun `existing results are skipped unless re-review is requested`() {
    val rule = rule()
    AiReviewRunClassifier
      .classify(candidate(Category.HAS_RESULT), context(rule))
      .kind.assert
      .isEqualTo(Kind.ALREADY_HAS_RESULT)
    val rereview = AiReviewRunClassifier.classify(candidate(Category.HAS_RESULT), context(rule, rereview = true))
    rereview.kind.assert.isEqualTo(Kind.REVIEW)
    rereview.rereviewOfAutoReviewed.assert.isFalse()
  }

  @Test
  fun `re-review of an AI approval runs only against active rules`() {
    val active = rule { threshold = 80 }
    val stricterDryRun =
      rule {
        mode = AiReviewRuleMode.DRY_RUN
        threshold = 95
      }
    AiReviewRunClassifier
      .classify(candidate(Category.AUTO_REVIEWED), context(active, stricterDryRun))
      .kind.assert
      .isEqualTo(Kind.ALREADY_HAS_RESULT)
    val decision =
      AiReviewRunClassifier.classify(
        candidate(Category.AUTO_REVIEWED),
        context(active, stricterDryRun, rereview = true),
      )
    decision.kind.assert.isEqualTo(Kind.REVIEW)
    decision.rule.assert.isEqualTo(active)
    decision.rereviewOfAutoReviewed.assert.isTrue()
    AiReviewRunClassifier
      .classify(candidate(Category.AUTO_REVIEWED), context(stricterDryRun, rereview = true))
      .kind.assert
      .isEqualTo(Kind.ALREADY_HAS_RESULT)
  }

  @Test
  fun `skips open QA issues only when the rule says so and QA covers the language`() {
    val skipping = rule()
    val notSkipping = rule { skipWithOpenQaIssues = false }
    val withIssues = candidate(hasOpenQaIssues = true)
    AiReviewRunClassifier
      .classify(withIssues, context(skipping, qaCovered = true))
      .kind.assert
      .isEqualTo(Kind.OPEN_QA_ISSUES)
    AiReviewRunClassifier
      .classify(withIssues, context(skipping, qaCovered = false))
      .kind.assert
      .isEqualTo(Kind.REVIEW)
    AiReviewRunClassifier
      .classify(withIssues, context(notSkipping, qaCovered = true))
      .kind.assert
      .isEqualTo(Kind.REVIEW)
  }

  @Test
  fun `the strictest rule decides, the scope rule only limits the selection`() {
    val uiRule =
      rule {
        tagIn = mutableListOf("ui")
        threshold = 80
      }
    val strictRule = rule { threshold = 95 }
    val decision = AiReviewRunClassifier.classify(candidate(), context(uiRule, strictRule, scopeRule = uiRule))
    decision.kind.assert.isEqualTo(Kind.REVIEW)
    decision.rule.assert.isEqualTo(strictRule)
    AiReviewRunClassifier
      .classify(candidate(scope = legal), context(uiRule, strictRule, scopeRule = uiRule))
      .kind.assert
      .isEqualTo(Kind.OUT_OF_SCOPE)
  }

  companion object {
    private const val LANGUAGE = 10L
    private const val OTHER_LANGUAGE = 11L
  }
}
