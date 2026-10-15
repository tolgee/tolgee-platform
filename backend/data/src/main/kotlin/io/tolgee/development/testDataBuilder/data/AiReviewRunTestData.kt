package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.model.Language
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.util.aiReviewTextHash

class AiReviewRunTestData : AiReviewTestData() {
  val uiRule: AiReviewRuleBuilder
  val legalRule: AiReviewRuleBuilder

  lateinit var failedGerman: TranslationBuilder
  lateinit var belowGerman: TranslationBuilder
  lateinit var dryRunBelowGerman: TranslationBuilder
  lateinit var approvedGerman: TranslationBuilder
  lateinit var approvedInvalidGerman: TranslationBuilder
  lateinit var staleGerman: TranslationBuilder
  lateinit var confirmedGerman: TranslationBuilder
  lateinit var qaGerman: TranslationBuilder
  lateinit var rejectedGerman: TranslationBuilder
  lateinit var pendingGerman: TranslationBuilder
  lateinit var queuedRereviewGerman: TranslationBuilder
  lateinit var legalFrench: TranslationBuilder
  lateinit var branchedLegalGerman: TranslationBuilder

  val failedKey: KeyBuilder
  val belowKey: KeyBuilder
  val dryRunBelowKey: KeyBuilder
  val approvedKey: KeyBuilder
  val approvedInvalidKey: KeyBuilder
  val staleKey: KeyBuilder
  val confirmedKey: KeyBuilder
  val qaKey: KeyBuilder
  val rejectedKey: KeyBuilder
  lateinit var pendingKey: KeyBuilder
  lateinit var queuedRereviewKey: KeyBuilder
  lateinit var branchedLegalKey: KeyBuilder

  val germanRuleRunner: UserAccount
  val czechRuleRunner: UserAccount

  init {
    project.useQaChecks = true
    uiRule =
      addRule {
        name = "UI"
        tagIn = mutableListOf("ui")
      }.neverForLanguage(czech.self)
    legalRule =
      addRule {
        name = "Legal"
        mode = AiReviewRuleMode.DRY_RUN
        tagIn = mutableListOf("legal")
      }

    uiGerman.self.qaChecksStale = false
    legalGerman.self.qaChecksStale = false
    uiFrench.self.state = TranslationState.REVIEWED

    failedKey = addUiKey("ui-failed", "Fehlgeschlagen") { failedGerman = withResult(AiReviewOutcome.FAILED) }
    belowKey = addUiKey("ui-below", "Darunter") { belowGerman = withResult(AiReviewOutcome.BELOW_THRESHOLD) }
    dryRunBelowKey =
      addUiKey("ui-dry-run-below", "Trocken darunter") {
        dryRunBelowGerman = withResult(AiReviewOutcome.BELOW_THRESHOLD, dryRun = true)
      }
    approvedKey =
      addUiKey("ui-approved", "Genehmigt FAKE_AI_REVIEW_MAJOR") {
        self.state = TranslationState.REVIEWED
        approvedGerman = withResult(AiReviewOutcome.AUTO_REVIEWED, score = 90)
      }
    approvedInvalidKey =
      addUiKey("ui-approved-invalid", "Genehmigt FAKE_AI_REVIEW_INVALID") {
        self.state = TranslationState.REVIEWED
        approvedInvalidGerman = withResult(AiReviewOutcome.AUTO_REVIEWED, score = 90)
      }
    staleKey =
      addUiKey("ui-stale", "Neuer Text") {
        staleGerman = withResult(AiReviewOutcome.BELOW_THRESHOLD, textHash = aiReviewTextHash("Alter Text"))
      }
    confirmedKey =
      addUiKey("ui-confirmed", "Bestätigt") {
        self.state = TranslationState.REVIEWED
        confirmedGerman = withResult(AiReviewOutcome.CONFIRMED)
      }
    qaKey =
      addUiKey("ui-qa", "Mit QA Problem") {
        qaGerman = this
        addQaIssue {
          type = QaCheckType.EMPTY_TRANSLATION
          message = QaIssueMessage.QA_EMPTY_TRANSLATION
          state = QaIssueState.OPEN
        }
      }
    rejectedKey = addUiKey("ui-rejected", "Abgelehnt") { rejectedGerman = withResult(AiReviewOutcome.REJECTED) }
    addUiKey("ui-empty", "") {}

    germanRuleRunner = addRuleRunner("ai_review_german_rule_runner", german.self)
    czechRuleRunner = addRuleRunner("ai_review_czech_rule_runner", czech.self)
  }

  private fun addRuleRunner(
    username: String,
    language: Language,
  ): UserAccount {
    val user = root.addUserAccount { this.username = username }.self
    projectBuilder.addPermission {
      this.user = user
      scopes =
        arrayOf(
          Scope.PROMPTS_EDIT,
          Scope.TRANSLATIONS_STATE_EDIT,
          Scope.BATCH_MACHINE_TRANSLATE,
        )
      viewLanguages = mutableSetOf(language)
      translateLanguages = mutableSetOf(language)
      stateChangeLanguages = mutableSetOf(language)
    }
    return user
  }

  val reviewedWithoutRereview get() = listOf(uiGerman, legalGerman, failedGerman, staleGerman)
  val dryRunOnly get() = listOf(legalGerman)
  val reviewedOnlyWithRereview get() = listOf(belowGerman, dryRunBelowGerman, approvedGerman, approvedInvalidGerman)
  val humanReviewed get() = listOf(uiFrench, confirmedGerman, rejectedGerman)
  val withoutRule get() = listOf(checkoutGerman, plainGerman)
  val never get() = listOf(uiCzech)
  val withOpenQaIssues get() = listOf(qaGerman)

  fun addQueuedReviews() {
    pendingKey = addUiKey("ui-pending", "Wartet") { pendingGerman = withResult(AiReviewOutcome.PENDING) }
    queuedRereviewKey =
      addUiKey("ui-queued-rereview", "Erneut genehmigt") {
        self.state = TranslationState.REVIEWED
        queuedRereviewGerman =
          withResult(AiReviewOutcome.PENDING, score = 90, previousOutcome = AiReviewOutcome.AUTO_REVIEWED)
      }
  }

  fun limitLegalRuleToGermanOnDefaultBranch() {
    legalRule.scopeLanguages(german.self)
    legalKey.build {
      legalFrench = addTranslation("fr", "Conditions").apply { self.qaChecksStale = false }
    }
    val featureBranch = projectBuilder.addBranch { name = "feature" }.self
    branchedLegalKey =
      projectBuilder
        .addKey {
          name = "legal-branched"
          branch = featureBranch
        }.build {
          addTag("legal")
          addTranslation("en", "Terms on a branch")
          branchedLegalGerman = addTranslation("de", "Bedingungen im Branch").apply { self.qaChecksStale = false }
        }
  }

  val allKeys: List<KeyBuilder>
    get() =
      listOf(
        uiKey,
        legalKey,
        checkoutKey,
        plainKey,
        failedKey,
        belowKey,
        dryRunBelowKey,
        approvedKey,
        approvedInvalidKey,
        staleKey,
        confirmedKey,
        qaKey,
        rejectedKey,
      )

  private fun addUiKey(
    name: String,
    germanText: String,
    ft: TranslationBuilder.() -> Unit,
  ): KeyBuilder =
    projectBuilder.addKey { this.name = name }.build {
      addTag("ui")
      addTranslation("en", name)
      addTranslation("de", germanText).apply {
        self.qaChecksStale = false
        ft()
      }
    }

  private fun TranslationBuilder.withResult(
    outcome: AiReviewOutcome,
    score: Int = 70,
    textHash: String = aiReviewTextHash(self.text),
    previousOutcome: AiReviewOutcome? = null,
    dryRun: Boolean = false,
  ): TranslationBuilder {
    addAiReview {
      this.outcome = outcome
      this.rule = uiRule.self
      this.textHash = textHash
      this.score = score
      this.previousOutcome = previousOutcome
      this.dryRun = dryRun
      threshold = 85
    }
    return this
  }
}
