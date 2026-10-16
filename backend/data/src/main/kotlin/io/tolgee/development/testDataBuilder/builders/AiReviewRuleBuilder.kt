package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.aiReview.AiReviewLanguageThreshold
import io.tolgee.model.aiReview.AiReviewRule

class AiReviewRuleBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<AiReviewRule, AiReviewRuleBuilder> {
  override var self: AiReviewRule = AiReviewRule(project = projectBuilder.self)

  private val scopeLanguages = mutableListOf<Language>()
  private val languageOverrides = mutableListOf<Triple<Language, Int?, Boolean>>()

  fun scopeLanguages(vararg languages: Language): AiReviewRuleBuilder {
    scopeLanguages.addAll(languages)
    return this
  }

  fun languageThreshold(
    language: Language,
    threshold: Int,
  ): AiReviewRuleBuilder {
    languageOverrides.add(Triple(language, threshold, false))
    return this
  }

  fun neverForLanguage(language: Language): AiReviewRuleBuilder {
    languageOverrides.add(Triple(language, null, true))
    return this
  }

  fun resolveLanguageReferences() {
    if (scopeLanguages.isNotEmpty()) {
      self.languageIds = scopeLanguages.map { it.id }.toMutableList()
    }
    if (languageOverrides.isNotEmpty()) {
      self.languageThresholds =
        languageOverrides
          .map { (language, threshold, never) ->
            AiReviewLanguageThreshold(languageId = language.id, threshold = threshold, never = never)
          }.toMutableList()
    }
  }
}
