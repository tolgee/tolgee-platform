package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.util.aiReviewTextHash

class AiReviewRuleParityTestData : AiReviewTestData() {
  val uiOrUntaggedRule: AiReviewRuleBuilder
  val taggedOutsideAdminRule: AiReviewRuleBuilder
  val checkoutDryRunRule: AiReviewRuleBuilder
  lateinit var autoReviewedNonAscii: TranslationBuilder

  init {
    uiOrUntaggedRule =
      addRule {
        name = "UI or untagged"
        tagIn = mutableListOf("ui", "")
        tagNotIn = mutableListOf("legal")
      }.scopeLanguages(german.self, czech.self).neverForLanguage(czech.self)
    taggedOutsideAdminRule =
      addRule {
        name = "Tagged outside admin"
        threshold = 90
        tagNotIn = mutableListOf("")
        namespaceNotIn = mutableListOf("admin")
      }.neverForLanguage(french.self)
    checkoutDryRunRule =
      addRule {
        name = "Checkout dry run"
        mode = AiReviewRuleMode.DRY_RUN
        namespaceIn = mutableListOf("", "checkout")
      }.scopeLanguages(french.self)

    val tagSets = listOf(listOf(), listOf("ui"), listOf("legal"), listOf("ui", "legal"), listOf("other"))
    val namespaces = listOf(null, "checkout", "admin")
    tagSets.forEachIndexed { tagIndex, tags ->
      namespaces.forEach { namespace ->
        projectBuilder.addKey { name = "parity-$tagIndex-${namespace ?: "default"}" }.build {
          setNamespace(namespace)
          tags.forEach { addTag(it) }
          addTranslation("en", "Size $tagIndex")
          addTranslation("de", "Größe $tagIndex")
          addTranslation("cs", "Žluťoučký kůň $tagIndex")
          addTranslation("fr", "Été $tagIndex")
        }
      }
    }

    projectBuilder.addKey { name = "hash-check" }.build {
      addTag("ui")
      addTranslation("en", "Greetings")
      autoReviewedNonAscii =
        addTranslation("de", "Grüße aus Köln").apply {
          self.state = TranslationState.REVIEWED
          addAiReview {
            outcome = AiReviewOutcome.AUTO_REVIEWED
            rule = uiOrUntaggedRule.self
            textHash = aiReviewTextHash(translation.text)
            score = 95
            threshold = 85
          }
        }
    }
  }
}
