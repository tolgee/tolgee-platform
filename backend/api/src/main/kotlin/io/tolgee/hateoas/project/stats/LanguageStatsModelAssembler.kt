package io.tolgee.hateoas.project.stats

import io.tolgee.api.v2.controllers.ProjectsController
import io.tolgee.model.LanguageStats
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LanguageStatsModelAssembler : RepresentationModelAssemblerSupport<LanguageStats, LanguageStatsModel>(
  ProjectsController::class.java,
  LanguageStatsModel::class.java,
) {
  override fun toModel(it: LanguageStats): LanguageStatsModel {
    return LanguageStatsModel(
      languageId = it.language.id,
      languageTag = it.language.tag,
      languageName = it.language.name,
      languageOriginalName = it.language.originalName,
      languageFlagEmoji = it.language.flagEmoji,
      translatedKeyCount = it.translatedKeys,
      translatedWordCount = it.translatedWords,
      translatedPercentage = it.translatedPercentage,
      reviewedKeyCount = it.reviewedKeys,
      reviewedWordCount = it.reviewedWords,
      reviewedPercentage = it.reviewedPercentage,
      untranslatedKeyCount = it.untranslatedKeys,
      untranslatedWordCount = it.untranslatedWords,
      untranslatedPercentage = it.untranslatedPercentage,
    )
  }
}
