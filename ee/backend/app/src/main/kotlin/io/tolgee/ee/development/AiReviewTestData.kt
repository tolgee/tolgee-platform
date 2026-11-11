package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationAiReviewBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.Language
import io.tolgee.model.Prompt
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation

open class AiReviewTestData : BaseTestData() {
  lateinit var german: Language
  lateinit var french: Language
  lateinit var key: Key
  lateinit var deTranslationBuilder: TranslationBuilder
  lateinit var frTranslationBuilder: TranslationBuilder
  lateinit var ruleBuilder: AiReviewRuleBuilder
  lateinit var translatePrompt: Prompt
  lateinit var reviewPrompt: Prompt
  lateinit var otherProjectLanguage: Language
  lateinit var otherProjectKey: Key
  lateinit var otherProjectTranslationBuilder: TranslationBuilder
  lateinit var otherProjectRuleBuilder: AiReviewRuleBuilder
  lateinit var otherProjectReviewPrompt: Prompt
  lateinit var translator: UserAccount
  lateinit var promptsViewer: UserAccount
  lateinit var englishTranslationBuilder: TranslationBuilder

  val deTranslation: Translation get() = deTranslationBuilder.self
  val frTranslation: Translation get() = frTranslationBuilder.self
  val rule: AiReviewRule get() = ruleBuilder.self

  init {
    project.useQaChecks = false
    project.useAiReview = true
    projectBuilder.build {
      german = addGerman().self
      french = addFrench().self
      key =
        addKey { name = "checkout.pay" }
          .build {
            addTag("ui")
            englishTranslationBuilder = addTranslation("en", "Pay now")
            deTranslationBuilder = addTranslation("de", "Jetzt bezahlen")
            frTranslationBuilder = addTranslation("fr", "Payer maintenant")
          }.self
      translatePrompt =
        addPrompt {
          name = "Translate"
          providerName = "default"
        }.self
      reviewPrompt =
        addPrompt {
          name = "Brand voice review"
          providerName = "default"
          type = PromptType.REVIEW
        }.self
      translator =
        root
          .addUserAccount { username = "ai_review_translator" }
          .self
          .also { user ->
            addPermission {
              this.user = user
              type = ProjectPermissionType.TRANSLATE
            }
          }
      promptsViewer =
        root
          .addUserAccount { username = "ai_review_prompts_viewer" }
          .self
          .also { user ->
            addPermission {
              this.user = user
              type = null
              scopes = arrayOf(Scope.PROMPTS_VIEW)
            }
          }
      ruleBuilder =
        addAiReviewRule {
          name = "UI"
          mode = AiReviewRuleMode.ACTIVE
          threshold = 85
          tagIn = mutableListOf("ui")
        }
    }
    root
      .addProject(userAccountBuilder.defaultOrganizationBuilder.self) { name = "Other project" }
      .build {
        self.useAiReview = true
        self.baseLanguage = addEnglish().self
        otherProjectLanguage = addGerman().self
        otherProjectKey =
          addKey { name = "other.key" }
            .build {
              addTag("ui")
              addTranslation("en", "Other")
              otherProjectTranslationBuilder = addTranslation("de", "Andere")
            }.self
        otherProjectRuleBuilder =
          addAiReviewRule {
            name = "Other UI"
            mode = AiReviewRuleMode.ACTIVE
            threshold = 85
            tagIn = mutableListOf("ui")
          }
        otherProjectReviewPrompt =
          addPrompt {
            name = "Other review"
            providerName = "default"
            type = PromptType.REVIEW
          }.self
      }
  }

  val otherProjectTranslation: Translation get() = otherProjectTranslationBuilder.self
  val otherProjectRule: AiReviewRule get() = otherProjectRuleBuilder.self

  fun addRule(ft: FT<AiReviewRule>): AiReviewRuleBuilder = projectBuilder.addAiReviewRule(ft)

  fun addBaseSuggestion(text: String): TranslationSuggestion =
    projectBuilder.data.keys
      .single { it.self === key }
      .addSuggestion {
        language = englishLanguage
        author = user
        translation = text
        state = TranslationSuggestionState.ACTIVE
      }.self

  fun addReview(
    translationBuilder: TranslationBuilder,
    status: AiReviewStatus,
    score: Int? = null,
    ft: FT<TranslationAiReview> = {},
  ): TranslationAiReviewBuilder =
    translationBuilder.addAiReview {
      this.status = status
      this.score = score
      ft(this)
    }

  class KeyTranslations(
    val keyBuilder: KeyBuilder,
    val de: TranslationBuilder,
    val fr: TranslationBuilder,
  ) {
    val key: Key get() = keyBuilder.self
  }

  fun addUserWithScopes(
    name: String,
    vararg scopes: Scope,
    stateChangeLanguages: Set<Language> = emptySet(),
  ): UserAccount =
    root
      .addUserAccount { username = name }
      .self
      .also { user ->
        projectBuilder.addPermission {
          this.user = user
          type = null
          this.scopes = scopes.toList().toTypedArray()
          if (stateChangeLanguages.isNotEmpty()) {
            this.stateChangeLanguages = stateChangeLanguages.toMutableSet()
          }
        }
      }

  fun addKeyWithTranslations(
    name: String,
    tags: List<String> = listOf("ui"),
    deText: String = "Deutscher Text",
    frText: String = "Texte français",
  ): KeyTranslations {
    lateinit var de: TranslationBuilder
    lateinit var fr: TranslationBuilder
    val keyBuilder =
      projectBuilder
        .addKey { this.name = name }
        .build {
          tags.forEach { addTag(it) }
          addTranslation("en", "English text")
          de = addTranslation("de", deText)
          fr = addTranslation("fr", frText)
        }
    return KeyTranslations(keyBuilder, de, fr)
  }

  fun addPluralKey(
    name: String,
    deText: String,
  ): TranslationBuilder {
    lateinit var de: TranslationBuilder
    projectBuilder
      .addKey {
        this.name = name
        isPlural = true
      }.build {
        addTag("ui")
        addTranslation("en", "{count, plural, one {# item} other {# items}}")
        de = addTranslation("de", deText)
      }
    return de
  }
}
