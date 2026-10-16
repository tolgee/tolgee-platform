package io.tolgee.ee.service.aiReview

import io.tolgee.model.Project
import io.tolgee.model.aiReview.AiReviewProjectSettings
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.SourceChangeAction
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.aiReview.AiReviewAvailability
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.service.translation.SourceChangeRetranslationPolicy
import io.tolgee.service.translation.SourceChangeRetranslationPolicyOssImpl
import io.tolgee.util.aiReviewTextHash
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class SourceChangeRetranslationPolicyEeImpl(
  private val ossPolicy: SourceChangeRetranslationPolicyOssImpl,
  private val aiReviewSettingsService: AiReviewSettingsService,
  private val aiReviewAvailability: AiReviewAvailability,
  private val projectService: ProjectService,
  private val translationAiReviewRepository: TranslationAiReviewRepository,
  @Lazy private val autoTranslationService: AutoTranslationService,
  private val entityManager: EntityManager,
) : SourceChangeRetranslationPolicy {
  override fun hasCustomBehavior(projectId: Long): Boolean = getCustomSettings(projectId) != null

  override fun selectForRetranslation(
    projectId: Long,
    translations: Collection<Translation>,
    includeHumanText: Boolean,
  ): Set<Long> {
    val settings =
      getCustomSettings(projectId) ?: return ossPolicy.selectForRetranslation(projectId, translations, includeHumanText)
    val aiApprovedIds = getAiApprovedIds(translations)
    val machineTranslatedLanguageIds by lazy { getMachineTranslatedLanguageIds(projectId, translations) }
    return translations
      .filter { translation ->
        when (classify(translation, aiApprovedIds)) {
          Kind.MACHINE_TRANSLATED -> true
          Kind.AI_APPROVED -> settings.onSourceChangeAiApproved == SourceChangeAction.RETRANSLATE
          Kind.HUMAN_EDITED ->
            includeHumanText &&
              settings.onSourceChangeHumanEdited == SourceChangeAction.RETRANSLATE &&
              translation.language.id in machineTranslatedLanguageIds
          Kind.HUMAN_REVIEWED ->
            includeHumanText &&
              settings.onSourceChangeHumanReviewed == SourceChangeAction.RETRANSLATE &&
              translation.language.id in machineTranslatedLanguageIds
        }
      }.map { it.id }
      .toSet()
  }

  private fun classify(
    translation: Translation,
    aiApprovedIds: Set<Long>,
  ): Kind {
    val aiApproved = translation.id in aiApprovedIds
    if (translation.auto) return if (aiApproved) Kind.AI_APPROVED else Kind.MACHINE_TRANSLATED
    val reviewedByHuman = translation.state == TranslationState.REVIEWED && !aiApproved
    return if (reviewedByHuman) Kind.HUMAN_REVIEWED else Kind.HUMAN_EDITED
  }

  private fun getCustomSettings(projectId: Long): AiReviewProjectSettings? {
    val settings = aiReviewSettingsService.find(projectId)?.takeUnless { it.isDefault } ?: return null
    val organizationId = projectService.findDto(projectId)?.organizationOwnerId ?: return null
    if (!aiReviewAvailability.isAvailable(organizationId)) return null
    return settings
  }

  private fun getAiApprovedIds(translations: Collection<Translation>): Set<Long> {
    val textHashes = translations.associate { it.id to aiReviewTextHash(it.text) }
    return translations
      .map { it.id }
      .chunked(IN_CLAUSE_CHUNK_SIZE)
      .flatMap { translationAiReviewRepository.findViewsByTranslationIds(it, listOf(AiReviewOutcome.AUTO_REVIEWED)) }
      .filter { it.textHash == textHashes[it.translationId] }
      .map { it.translationId }
      .toSet()
  }

  private fun getMachineTranslatedLanguageIds(
    projectId: Long,
    translations: Collection<Translation>,
  ): Set<Long> {
    val project = entityManager.getReference(Project::class.java, projectId)
    val languageIds = translations.map { it.language.id }.distinct()
    return autoTranslationService
      .getConfigs(project, languageIds)
      .filterValues { it.usingPrimaryMtService }
      .keys
  }

  private enum class Kind {
    MACHINE_TRANSLATED,
    AI_APPROVED,
    HUMAN_EDITED,
    HUMAN_REVIEWED,
  }

  companion object {
    private const val IN_CLAUSE_CHUNK_SIZE = 10_000
  }
}
