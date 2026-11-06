package io.tolgee.component

import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.events.OnTranslationsSet
import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.enums.SourceChangeAction
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import io.tolgee.service.aiReview.AiReviewRowAccess
import io.tolgee.service.aiReview.AiReviewRowState
import io.tolgee.service.aiReview.EffectiveSourceChangeSettings
import io.tolgee.service.aiReview.SourceChangeDecision
import io.tolgee.service.aiReview.SourceChangeSettingsProvider
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Decides what happens to each translation when the base text changes. This must run before the
 * auto-translate job: by the time softAutoTranslate runs, the original state is gone.
 */
@Component
class OutdatedFlagListener(
  private val translationService: TranslationService,
  private val sourceChangeSettingsProvider: SourceChangeSettingsProvider,
  private val aiReviewRowAccess: AiReviewRowAccess,
  private val projectFeatureGuard: ProjectFeatureGuard,
  @Lazy
  private val autoTranslationService: AutoTranslationService,
) {
  private enum class Kind {
    AI_APPROVED,
    HUMAN_REVIEWED,
    MACHINE,
    HUMAN_EDITED,
  }

  @EventListener
  @Order(1)
  fun onEvent(event: OnTranslationsSet) {
    val project = event.key.project
    val baseLanguage = project.baseLanguage ?: return
    val oldBaseValue = event.oldValues[baseLanguage.tag]
    val newBaseValue = event.translations.find { it.language.id == baseLanguage.id }?.text
    if (oldBaseValue == newBaseValue) return

    val excluded = event.translations.map { it.id }.toSet()
    val projectDto = ProjectDto.fromEntity(project)
    val settings = sourceChangeSettingsProvider.getEffective(projectDto)
    val protect = project.translationProtection == TranslationProtection.PROTECT_REVIEWED
    val configs by lazy { autoTranslationService.getConfigs(project) }
    val rowStates = reviewRowStates(event, excluded, baseLanguage.id, projectDto)

    translationService.setOutdated(event.key, excluded) { translation ->
      decide(translation, settings, protect, configs, rowStates[translation.id] ?: AiReviewRowState.NONE)
    }
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

  private fun decide(
    translation: Translation,
    settings: EffectiveSourceChangeSettings,
    protect: Boolean,
    configs: List<AutoTranslationConfig>,
    rowState: AiReviewRowState,
  ): SourceChangeDecision {
    val kind = kindOf(translation, rowState)
    val keepsState = protect && translation.state == TranslationState.REVIEWED
    if (kind == Kind.MACHINE) return keepOrDropState(keepsState)

    var action =
      when (kind) {
        Kind.AI_APPROVED -> settings.aiApproved
        Kind.HUMAN_REVIEWED -> settings.humanReviewed
        else -> settings.humanEdited
      }
    // Nothing may be marked auto that no auto translation will replace.
    if (action == SourceChangeAction.RETRANSLATE && !autoTranslationEnabled(translation, configs)) {
      action = SourceChangeAction.MARK_OUTDATED
    }

    if (action == SourceChangeAction.RETRANSLATE) {
      if (keepsState) return SourceChangeDecision(newState = null)
      return SourceChangeDecision(
        newState = TranslationState.TRANSLATED,
        setAuto = true,
        deleteAiReviewRow = true,
      )
    }

    val dropsToTranslated = translation.state == TranslationState.REVIEWED && !protect
    // "Keep" on an AI approval must clear auto, or softAutoTranslate would still replace the text.
    val clearAuto = kind == Kind.AI_APPROVED && !protect
    return SourceChangeDecision(
      newState =
        when {
          keepsState -> null
          else -> TranslationState.TRANSLATED
        },
      setAuto =
        when {
          clearAuto -> false
          else -> null
        },
      // A Translated string must never show "Reviewed by AI".
      deleteAiReviewRow = dropsToTranslated && rowState.currentApproval,
    )
  }

  /**
   * Spec order, first match wins. A human-written text that the AI approved has no auto flag and
   * falls through to "Edited by a human": an AI approval does not count as a human touch.
   */
  private fun kindOf(
    translation: Translation,
    rowState: AiReviewRowState,
  ): Kind {
    if (rowState.aiManaged && translation.auto) return Kind.AI_APPROVED
    if (translation.state == TranslationState.REVIEWED && !rowState.aiManaged) return Kind.HUMAN_REVIEWED
    if (translation.auto) return Kind.MACHINE
    return Kind.HUMAN_EDITED
  }

  private fun keepOrDropState(keepsState: Boolean): SourceChangeDecision {
    if (keepsState) return SourceChangeDecision(newState = null)
    return SourceChangeDecision(newState = TranslationState.TRANSLATED)
  }

  private fun autoTranslationEnabled(
    translation: Translation,
    configs: List<AutoTranslationConfig>,
  ): Boolean {
    val config =
      configs.find { it.targetLanguage?.id == translation.language.id }
        ?: configs.find { it.targetLanguage == null }
        ?: return false
    return config.usingTm || config.usingPrimaryMtService
  }
}
