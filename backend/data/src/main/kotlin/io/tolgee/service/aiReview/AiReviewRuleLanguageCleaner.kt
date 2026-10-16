package io.tolgee.service.aiReview

import io.tolgee.model.Language
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import org.springframework.stereotype.Component

@Component
class AiReviewRuleLanguageCleaner(
  private val aiReviewRuleRepository: AiReviewRuleRepository,
) {
  fun removeLanguage(language: Language) {
    aiReviewRuleRepository.findAllByProjectId(language.project.id).forEach { rule ->
      val scopedToLanguage = language.id in rule.languageIds
      val hasOverride = rule.languageThresholds.any { it.languageId == language.id }
      if (!scopedToLanguage && !hasOverride) return@forEach

      rule.languageThresholds = rule.languageThresholds.filter { it.languageId != language.id }.toMutableList()
      if (scopedToLanguage) {
        rule.languageIds = rule.languageIds.filter { it != language.id }.toMutableList()
        if (rule.languageIds.isEmpty()) {
          rule.enabled = false
        }
      }
      aiReviewRuleRepository.save(rule)
    }
  }
}
