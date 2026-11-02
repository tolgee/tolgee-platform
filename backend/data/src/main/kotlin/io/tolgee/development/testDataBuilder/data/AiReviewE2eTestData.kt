package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.model.Language
import io.tolgee.model.Prompt
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus

class AiReviewE2eTestData : BaseTestData("test_username", "ai_review_project") {
  lateinit var german: Language
  lateinit var french: Language
  lateinit var reviewer: UserAccount
  lateinit var translator: UserAccount
  lateinit var reviewPrompt: Prompt
  lateinit var activeRule: AiReviewRule
  lateinit var dryRunRule: AiReviewRule
  lateinit var approvedTranslation: TranslationBuilder

  init {
    project.useQaChecks = false

    projectBuilder.build {
      german = addGerman().self
      french = addFrench().self

      reviewer =
        root
          .addUserAccount { username = "reviewer_username" }
          .self
          .also { user ->
            addPermission {
              this.user = user
              type = ProjectPermissionType.REVIEW
            }
          }
      translator =
        root
          .addUserAccount { username = "translator_username" }
          .self
          .also { user ->
            addPermission {
              this.user = user
              type = ProjectPermissionType.TRANSLATE
            }
          }

      reviewPrompt =
        addPrompt {
          name = "Brand review"
          providerName = "default"
          type = PromptType.REVIEW
          reviewCriteria = "Use informal language."
        }.self

      activeRule =
        addAiReviewRule {
          name = "UI rule"
          mode = AiReviewRuleMode.ACTIVE
          threshold = 85
          tagIn = mutableListOf("ui")
        }.self
      dryRunRule =
        addAiReviewRule {
          name = "Legal dry run"
          mode = AiReviewRuleMode.DRY_RUN
          threshold = 90
          tagIn = mutableListOf("legal")
        }.self

      approvedTranslation =
        addReviewedKey("key_approved", AiReviewStatus.APPROVED, 92) {
          rule = activeRule
          threshold = 85
          promptName = "Default review prompt"
          providerName = "server-provider"
          model = "gpt-test"
        }
      addReviewedKey("key_below_threshold", AiReviewStatus.BELOW_THRESHOLD, 40, reviewed = false) {
        rule = activeRule
        threshold = 85
      }
      addReviewedKey("key_dry_run_pass", AiReviewStatus.DRY_RUN_PASS, 90, reviewed = false, tag = "legal") {
        rule = dryRunRule
        threshold = 90
        ruleMode = AiReviewRuleMode.DRY_RUN
      }
      addReviewedKey("key_dry_run_fail", AiReviewStatus.DRY_RUN_FAIL, 30, reviewed = false, tag = "legal") {
        rule = dryRunRule
        threshold = 90
        ruleMode = AiReviewRuleMode.DRY_RUN
      }
      addReviewedKey("key_failed", AiReviewStatus.FAILED, null, reviewed = false) {
        rule = activeRule
        failureReason = AiReviewFailureReason.PROVIDER_ERROR
      }
      addReviewedKey("key_pending", AiReviewStatus.PENDING, null, reviewed = false) {
        rule = activeRule
      }
      addReviewedKey("key_skipped_qa", AiReviewStatus.SKIPPED_QA_ISSUES, null, reviewed = false) {
        rule = activeRule
      }
      addReviewedKey("key_last_failed", AiReviewStatus.APPROVED, 88) {
        rule = activeRule
        threshold = 85
        lastFailureReason = AiReviewFailureReason.PROVIDER_ERROR
      }

      addKey { name = "key_untagged" }.build {
        addTranslation("en", "Untagged source")
        addTranslation("de", "Text ohne Tags")
        addTranslation("fr", "Texte sans tags")
      }
      addKey { name = "key_not_reviewed" }.build {
        addTag("ui")
        addTranslation("en", "Not reviewed source")
        addTranslation("de", "Noch nicht geprüft")
        addTranslation("fr", "Pas encore vérifié")
      }
    }
  }

  private fun addReviewedKey(
    keyName: String,
    status: AiReviewStatus,
    score: Int?,
    reviewed: Boolean = true,
    tag: String = "ui",
    ft: TranslationAiReview.() -> Unit,
  ): TranslationBuilder {
    lateinit var de: TranslationBuilder
    projectBuilder
      .addKey { name = keyName }
      .build {
        addTag(tag)
        addTranslation("en", "Source of $keyName")
        de = addTranslation("de", "Deutsch für $keyName")
        addTranslation("fr", "Français pour $keyName")
      }
    if (reviewed) {
      de.self.state = TranslationState.REVIEWED
      de.self.auto = true
    }
    de.addAiReview {
      this.status = status
      this.score = score
      ft(this)
    }
    return de
  }
}
