package io.tolgee.development.testDataBuilder.data

import io.tolgee.activity.data.ActivityType
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.model.Language
import io.tolgee.model.Prompt
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.SystemActor
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import java.util.Date

class AiReviewE2eTestData : BaseTestData("test_username", "ai_review_project") {
  lateinit var german: Language
  lateinit var french: Language
  lateinit var reviewer: UserAccount
  lateinit var translator: UserAccount
  lateinit var promptsViewer: UserAccount
  lateinit var reviewPrompt: Prompt
  lateinit var activeRule: AiReviewRule
  lateinit var scoreOnlyRunRule: AiReviewRule
  lateinit var approvedTranslation: TranslationBuilder

  init {
    user.name = "Test User"
    project.useQaChecks = false
    project.useAiReview = true

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
              type = null
              scopes =
                arrayOf(
                  Scope.KEYS_VIEW,
                  Scope.TRANSLATIONS_VIEW,
                  Scope.TRANSLATIONS_STATE_EDIT,
                  Scope.PROMPTS_VIEW,
                  Scope.PROMPTS_EDIT,
                  Scope.BATCH_MACHINE_TRANSLATE,
                )
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
      promptsViewer =
        root
          .addUserAccount { username = "prompts_viewer_username" }
          .self
          .also { user ->
            addPermission {
              this.user = user
              type = null
              scopes =
                arrayOf(
                  Scope.KEYS_VIEW,
                  Scope.TRANSLATIONS_VIEW,
                  Scope.PROMPTS_VIEW,
                )
            }
          }

      reviewPrompt =
        addPrompt {
          name = "Brand review"
          providerName = "default"
          type = PromptType.REVIEW
        }.self
      addPrompt {
        name = "Plain translate"
        providerName = "default"
        type = PromptType.TRANSLATE
      }

      activeRule =
        addAiReviewRule {
          name = "UI rule"
          mode = AiReviewRuleMode.ACTIVE
          threshold = 85
          tagIn = mutableListOf("ui")
          prompt = reviewPrompt
        }.self
      scoreOnlyRunRule =
        addAiReviewRule {
          name = "Legal score only"
          mode = AiReviewRuleMode.SCORE_ONLY
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
      addReviewedKey("key_score_only_pass", AiReviewStatus.SCORE_ONLY_PASS, 90, reviewed = false, tag = "legal") {
        rule = scoreOnlyRunRule
        threshold = 90
        ruleMode = AiReviewRuleMode.SCORE_ONLY
      }
      addReviewedKey("key_score_only_fail", AiReviewStatus.SCORE_ONLY_FAIL, 30, reviewed = false, tag = "legal") {
        rule = scoreOnlyRunRule
        threshold = 90
        ruleMode = AiReviewRuleMode.SCORE_ONLY
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
      addReviewedKey("key_overridden_after_failed_rereview", AiReviewStatus.OVERRIDDEN, 92, reviewed = false) {
        rule = activeRule
        threshold = 85
        resolvedBy = user
        resolvedAt = Date()
        lastFailureReason = AiReviewFailureReason.PROVIDER_ERROR
      }
      addReviewedKey("key_rule_deleted", AiReviewStatus.APPROVED, 91) {
        threshold = 85
      }
      addReviewedKey("key_protect_below", AiReviewStatus.BELOW_THRESHOLD, 40) {
        rule = activeRule
        threshold = 85
      }
      addReviewedKey("key_human_text_below", AiReviewStatus.BELOW_THRESHOLD, 40, auto = false) {
        rule = activeRule
        threshold = 85
      }
      addReviewedKey("key_human_failed", AiReviewStatus.FAILED, null, auto = false) {
        rule = scoreOnlyRunRule
        ruleMode = AiReviewRuleMode.SCORE_ONLY
        failureReason = AiReviewFailureReason.PROVIDER_ERROR
      }
      addReviewedKey("key_with_issues", AiReviewStatus.BELOW_THRESHOLD, 55, reviewed = false) {
        rule = activeRule
        threshold = 85
        promptName = "Default review prompt"
        providerName = "server-provider"
        model = "gpt-test"
        issues =
          mutableListOf(
            AiReviewIssue("critical", "accuracy", "Wrong meaning", 35),
            AiReviewIssue("minor", "style", "Awkward wording", 10),
          )
      }

      val untaggedKey = addKey { name = "key_untagged" }
      untaggedKey.build {
        addTranslation("en", "Untagged source")
        addTranslation("de", "Text ohne Tags")
        addTranslation("fr", "Texte sans tags")
      }
      addAiPlaygroundResult {
        this.user = this@AiReviewE2eTestData.user
        this.project = projectBuilder.self
        this.key = untaggedKey.self
        this.language = german
        this.score = 55
        this.issues =
          mutableListOf(
            AiReviewIssue("critical", "accuracy", "Wrong meaning", 35),
            AiReviewIssue("minor", "style", "Awkward wording", 10),
          )
      }
      addKey { name = "key_not_reviewed" }.build {
        addTag("ui")
        addTranslation("en", "Not reviewed source")
        addTranslation("de", "Noch nicht geprüft")
        addTranslation("fr", "Pas encore vérifié")
      }
    }
    addManualRunRevision()

    root
      .addProject(userAccountBuilder.defaultOrganizationBuilder.self) { name = "ai_review_empty_project" }
      .build {
        self.translationProtection = TranslationProtection.PROTECT_REVIEWED
        self.useAiReview = true
        self.baseLanguage = addEnglish().self
        addGerman()
        addKey { name = "plain_key" }.build {
          addTranslation("en", "Plain source")
          addTranslation("de", "Einfacher Text")
        }
      }

    root
      .addProject(userAccountBuilder.defaultOrganizationBuilder.self) { name = "ai_review_switch_project" }
      .build {
        self.baseLanguage = addEnglish().self
        addGerman()
        addKey { name = "switch_key" }.build {
          addTranslation("en", "Switch source")
          addTranslation("de", "Schalter Text")
        }
      }

    root
      .addProject(userAccountBuilder.defaultOrganizationBuilder.self) { name = "ai_review_branching_project" }
      .build {
        self.useBranching = true
        self.useQaChecks = true
        self.useAiReview = true
        self.baseLanguage = addEnglish().self
        addGerman()
        addFrench()
        val mainBranch =
          addBranch {
            name = "main"
            project = self
            isDefault = true
            isProtected = true
          }.self
        addBranch {
          name = "release"
          project = self
        }
        addKey {
          name = "branch_key_ui"
          branch = mainBranch
        }.build {
          addTag("ui")
          addTranslation("en", "Branch source ui")
          addTranslation("de", "Branch Text ui")
        }
        addKey {
          name = "branch_key_legal"
          branch = mainBranch
        }.build {
          addTag("legal")
          addTranslation("en", "Branch source legal")
          addTranslation("de", "Branch Text legal")
        }
      }
  }

  private fun addManualRunRevision() {
    projectBuilder
      .addActivityRevision {
        type = ActivityType.AI_REVIEW
        systemActor = SystemActor.AI_REVIEWER
      }.also { builder ->
        builder.authorIdProvider = { user.id }
        builder.addModifiedEntity("Translation") { approvedTranslation.self.id }
      }
  }

  private fun addReviewedKey(
    keyName: String,
    status: AiReviewStatus,
    score: Int?,
    reviewed: Boolean = true,
    auto: Boolean = true,
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
      de.self.auto = auto
    }
    de.addAiReview {
      this.status = status
      this.score = score
      if (status != AiReviewStatus.PENDING && status != AiReviewStatus.SKIPPED_QA_ISSUES) {
        this.reviewedAt = Date()
      }
      ft(this)
    }
    return de
  }
}
