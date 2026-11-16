package io.tolgee.component

import io.tolgee.activity.ActivityHolder
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.events.OnTranslationsSet
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.service.aiReview.AiReviewRowAccess
import io.tolgee.service.aiReview.AiReviewRowState
import io.tolgee.service.aiReview.SourceChangePolicy
import io.tolgee.service.aiReview.SourceChangeSettingsProvider
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * This must run before the auto-translate job: by the time softAutoTranslate runs, the original
 * state is gone.
 */
@Component
class SourceChangeListener(
  private val translationService: TranslationService,
  private val sourceChangeSettingsProvider: SourceChangeSettingsProvider,
  private val aiReviewRowAccess: AiReviewRowAccess,
  private val projectFeatureGuard: ProjectFeatureGuard,
  @Lazy
  private val autoTranslationService: AutoTranslationService,
  private val activityHolder: ActivityHolder,
) {
  @EventListener
  @Order(1)
  fun applySourceChangeToSiblings(event: OnTranslationsSet) {
    val project = event.key.project
    val baseLanguage = project.baseLanguage ?: return
    val oldBaseValue = event.oldValues[baseLanguage.tag]
    val newBaseValue = event.translations.find { it.language.id == baseLanguage.id }?.text
    if (oldBaseValue == newBaseValue) return

    val excluded = event.translations.map { it.id }.toSet()
    val projectDto = ProjectDto.fromEntity(project)
    val settings = sourceChangeSettingsProvider.getEffective(projectDto)
    val protect = project.translationProtection == TranslationProtection.PROTECT_REVIEWED
    val autoTranslatedLanguageIds by lazy { autoTranslatedLanguageIds(event, newBaseValue) }
    val rowStates = reviewRowStates(event, excluded, baseLanguage.id, projectDto)

    translationService.applySourceChange(event.key, excluded) { translation ->
      SourceChangePolicy.decide(
        translation,
        settings,
        protect,
        { translation.language.id in autoTranslatedLanguageIds },
        rowStates[translation.id] ?: AiReviewRowState.NONE,
      )
    }
  }

  private fun autoTranslatedLanguageIds(
    event: OnTranslationsSet,
    newBaseValue: String?,
  ): Set<Long> {
    if (newBaseValue.isNullOrBlank()) return emptySet()
    val project = event.key.project
    val activityType = activityHolder.activityRevision.type
    if (!autoTranslationService.startsForActivity(activityType, autoTranslationService.getConfigs(project))) {
      return emptySet()
    }
    val languageIds =
      event.key.translations
        .map { it.language.id }
        .distinct()
    return autoTranslationService
      .getConfigs(project, languageIds)
      .filterValues { autoTranslationService.translatesLanguage(it) }
      .keys
  }

  private fun reviewRowStates(
    event: OnTranslationsSet,
    excluded: Set<Long>,
    baseLanguageId: Long,
    projectDto: ProjectDto,
  ): Map<Long, AiReviewRowState> {
    if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, projectDto)) return emptyMap()
    val siblings = event.key.translations.filter { it.id !in excluded && it.language.id != baseLanguageId }
    return aiReviewRowAccess.reviewStates(siblings)
  }
}
