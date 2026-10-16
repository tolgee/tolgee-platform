package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.LanguageBuilder
import io.tolgee.development.testDataBuilder.builders.PromptBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.aiReview.AiReviewRuleMode

open class AiReviewTestData : BaseTestData("ai_review_owner", "AI review project") {
  lateinit var german: LanguageBuilder
  lateinit var czech: LanguageBuilder
  lateinit var french: LanguageBuilder

  lateinit var translator: UserAccount
  lateinit var czechReviewer: UserAccount
  lateinit var promptViewer: UserAccount

  lateinit var uiKey: KeyBuilder
  lateinit var legalKey: KeyBuilder
  lateinit var checkoutKey: KeyBuilder
  lateinit var plainKey: KeyBuilder

  lateinit var uiGerman: TranslationBuilder
  lateinit var uiCzech: TranslationBuilder
  lateinit var uiFrench: TranslationBuilder
  lateinit var legalGerman: TranslationBuilder
  lateinit var checkoutGerman: TranslationBuilder
  lateinit var plainGerman: TranslationBuilder

  lateinit var reviewPrompt: PromptBuilder
  lateinit var translatePrompt: PromptBuilder

  init {
    project.useQaChecks = false
    projectBuilder.build {
      german = addGerman()
      czech = addCzech()
      french = addFrench()

      reviewPrompt =
        addPrompt {
          name = "Strict review"
          providerName = "default"
          type = PromptType.REVIEW
          reviewCriteria = "Be strict about tone."
        }

      translatePrompt =
        addPrompt {
          name = "Translate prompt"
          providerName = "default"
        }

      uiKey =
        addKey { name = "ui-key" }.build {
          addTag("ui")
          addTranslation("en", "Pay now")
          uiGerman = addTranslation("de", "Jetzt zahlen")
          uiCzech = addTranslation("cs", "Zaplatit")
          uiFrench = addTranslation("fr", "Payer")
        }

      legalKey =
        addKey { name = "legal-key" }.build {
          addTag("legal")
          addTranslation("en", "Terms")
          legalGerman = addTranslation("de", "Bedingungen")
        }

      checkoutKey =
        addKey { name = "checkout-key" }.build {
          setNamespace("checkout")
          addTranslation("en", "Checkout")
          checkoutGerman = addTranslation("de", "Kasse")
        }

      plainKey =
        addKey { name = "plain-key" }.build {
          addTranslation("en", "Hello")
          plainGerman = addTranslation("de", "Hallo")
        }
    }

    translator =
      root
        .addUserAccount {
          username = "ai_review_translator"
        }.self
    projectBuilder.addPermission {
      user = translator
      type = ProjectPermissionType.TRANSLATE
    }

    czechReviewer =
      root
        .addUserAccount {
          username = "ai_review_czech_reviewer"
        }.self
    projectBuilder.addPermission {
      user = czechReviewer
      type = ProjectPermissionType.REVIEW
      viewLanguages = mutableSetOf(czech.self, german.self)
      translateLanguages = mutableSetOf(czech.self)
      stateChangeLanguages = mutableSetOf(czech.self)
    }

    promptViewer =
      root
        .addUserAccount {
          username = "ai_review_prompt_viewer"
        }.self
    projectBuilder.addPermission {
      user = promptViewer
      scopes = arrayOf(Scope.PROMPTS_VIEW)
    }
  }

  fun addRule(ft: AiReviewRule.() -> Unit = {}): AiReviewRuleBuilder =
    projectBuilder.addAiReviewRule {
      mode = AiReviewRuleMode.ACTIVE
      threshold = 85
      ft()
    }

  fun enableAutoTranslation() {
    projectBuilder.addAutoTranslationConfig {
      usingPrimaryMtService = true
    }
  }
}
