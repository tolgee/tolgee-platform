package io.tolgee.hateoas.project.stats

import io.tolgee.api.ILanguageStats
import io.tolgee.api.v2.controllers.project.ProjectsController
import io.tolgee.dtos.cacheable.LanguageDto
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LanguageStatsModelAssembler :
  RepresentationModelAssemblerSupport<Pair<ILanguageStats, LanguageDto>, LanguageStatsModel>(
    ProjectsController::class.java,
    LanguageStatsModel::class.java,
  ) {
  override fun toModel(it: Pair<ILanguageStats, LanguageDto>): LanguageStatsModel {
    val language = it.second
    val stats = it.first
    return LanguageStatsModel(
      languageId = language.id,
      languageTag = language.tag,
      languageName = language.name,
      languageOriginalName = language.originalName,
      languageFlagEmoji = language.flagEmoji,
      translatedKeyCount = stats.translatedKeys,
      translatedWordCount = stats.translatedWords,
      translatedPercentage = stats.translatedPercentage,
      reviewedKeyCount = stats.reviewedKeys,
      reviewedWordCount = stats.reviewedWords,
      reviewedPercentage = stats.reviewedPercentage,
      untranslatedKeyCount = stats.untranslatedKeys,
      untranslatedWordCount = stats.untranslatedWords,
      untranslatedPercentage = stats.untranslatedPercentage,
      translationsUpdatedAt = stats.translationsUpdatedAt,
    )
  }
}
