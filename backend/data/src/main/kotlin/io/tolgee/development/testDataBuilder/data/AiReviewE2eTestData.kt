package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.util.aiReviewTextHash
import java.util.Date

class AiReviewE2eTestData : AiReviewTestData() {
  val uiRule: AiReviewRuleBuilder
  val legalRule: AiReviewRuleBuilder

  init {
    root.addUserAccount {
      username = "ai_review_admin"
      name = "AI review admin"
      role = UserAccount.Role.ADMIN
    }

    uiRule =
      addRule {
        name = "UI rule"
        threshold = 80
        tagIn = mutableListOf("ui")
        prompt = reviewPrompt.self
      }

    legalRule =
      addRule {
        name = "Legal dry run"
        mode = AiReviewRuleMode.DRY_RUN
        threshold = 80
        tagIn = mutableListOf("legal")
        branchNotIn = mutableListOf("draft/*")
      }

    uiGerman.self.state = TranslationState.REVIEWED
    uiGerman.seedReview(uiRule, AiReviewOutcome.AUTO_REVIEWED) {
      score = 95
      issues = mutableListOf(issue(AiReviewIssueSeverity.MINOR, "Slightly formal", 5))
    }

    uiCzech.seedReview(uiRule, AiReviewOutcome.BELOW_THRESHOLD) {
      score = 70
      issues =
        mutableListOf(
          issue(AiReviewIssueSeverity.MAJOR, "Wrong verb form", 25),
          issue(AiReviewIssueSeverity.MINOR, "Missing emphasis", 5),
        )
    }

    uiFrench.seedReview(uiRule, AiReviewOutcome.FAILED) {
      providerName = null
      errorCode = "ai_review_invalid_output"
      errorDetail = "not a json"
    }

    legalGerman.seedReview(legalRule, AiReviewOutcome.WOULD_AUTO_REVIEW) {
      dryRun = true
      score = 100
      prompt = null
      issues = mutableListOf()
    }
  }

  private fun TranslationBuilder.seedReview(
    rule: AiReviewRuleBuilder,
    outcome: AiReviewOutcome,
    ft: TranslationAiReview.() -> Unit,
  ) {
    val translation = self
    addAiReview {
      this.outcome = outcome
      textHash = aiReviewTextHash(translation.text)
      this.rule = rule.self
      prompt = rule.self.prompt
      providerName = "server-provider"
      threshold = rule.self.threshold
      reviewedAt = Date()
      ft()
    }
  }

  private fun issue(
    severity: AiReviewIssueSeverity,
    message: String,
    points: Int,
  ) = AiReviewIssue(
    severity = severity,
    category = "style",
    message = message,
    points = points,
  )
}
