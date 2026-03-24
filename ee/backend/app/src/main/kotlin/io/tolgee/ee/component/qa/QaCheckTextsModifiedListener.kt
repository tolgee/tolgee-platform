package io.tolgee.ee.component.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.constants.Feature
import io.tolgee.events.OnTranslationTextsModified
import io.tolgee.model.Project
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaCheckTextsModifiedListener(
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val translationService: TranslationService,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onTranslationTextsModified(event: OnTranslationTextsModified) {
    if (event.translationIds.isEmpty()) return

    val project = projectService.getDto(event.projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

    val directTargets = getDirectTargets(event)
    val siblingTargets = getSiblingTargetsAndMarkAsStale(event)

    val allTargets = (directTargets + siblingTargets).distinctBy { it.keyId to it.languageId }
    if (allTargets.isEmpty()) return

    batchJobService.startJob(
      request = QaCheckRequest(target = allTargets),
      project = entityManager.getReference(Project::class.java, event.projectId),
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }

  private fun getDirectTargets(event: OnTranslationTextsModified): List<BatchTranslationTargetItem> {
    return translationService
      .getKeyLanguagePairsByTranslationIds(event.translationIds)
      .map { BatchTranslationTargetItem(keyId = it.keyId, languageId = it.languageId) }
  }

  private fun getSiblingTargetsAndMarkAsStale(event: OnTranslationTextsModified): List<BatchTranslationTargetItem> {
    val baseLanguage = languageService.getProjectBaseLanguage(event.projectId)

    // Find keys where the base language text was modified and expand to all project languages for those keys
    val baseChangedKeyIds = translationService.getBaseLanguageKeyIds(event.translationIds, baseLanguage.id)

    if (baseChangedKeyIds.isEmpty()) return emptyList()

    // We need to mark existing sibling translations (translations of modified base translation) as stale
    val siblingIds =
      translationService.getSiblingIdsForBaseLanguageChanges(
        translationIds = event.translationIds,
        baseLanguageId = baseLanguage.id,
      )
    if (siblingIds.isNotEmpty()) {
      translationService.setQaChecksStale(siblingIds)
    }

    // Expand the targets to project languages - includes languages for which there is no translation yet
    val allLanguageIds = languageService.getProjectLanguages(event.projectId).map { it.id }
    return baseChangedKeyIds.flatMap { keyId ->
      allLanguageIds
        .filter { it != baseLanguage.id }
        .map { langId -> BatchTranslationTargetItem(keyId = keyId, languageId = langId) }
    }
  }
}
