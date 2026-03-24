package io.tolgee.ee.component.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.constants.Feature
import io.tolgee.events.OnTranslationsSet
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaCheckTranslationListener(
  private val batchJobService: BatchJobService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val translationService: TranslationService,
  private val languageService: LanguageService,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onTranslationsSet(event: OnTranslationsSet) {
    val project = event.key.project
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

    if (event.translations.isEmpty()) return

    val keyId = event.key.id
    val directTargets =
      event.translations.map { BatchTranslationTargetItem(keyId = keyId, languageId = it.language.id) }

    val siblingTargets = getSiblingTargetsForBaseTextChange(event, directTargets.map { it.languageId }.toSet())

    val allTargets = (directTargets + siblingTargets).distinctBy { it.keyId to it.languageId }

    batchJobService.startJob(
      request = QaCheckRequest(target = allTargets),
      project = project,
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }

  private fun getSiblingTargetsForBaseTextChange(
    event: OnTranslationsSet,
    excludedLanguageIds: Set<Long>,
  ): List<BatchTranslationTargetItem> {
    val baseLanguage = event.key.project.baseLanguage ?: return emptyList()

    val oldBaseValue = event.oldValues[baseLanguage.tag]
    val newBaseValue = event.translations.find { it.language.id == baseLanguage.id }?.text
    val baseTextUnchanged = oldBaseValue == newBaseValue

    if (baseTextUnchanged) return emptyList()

    // We need to mark existing sibling translations as stale
    val existingSiblings =
      event.key.translations
        .filter { it.language.id != baseLanguage.id && it.language.id !in excludedLanguageIds }
        .distinctBy { it.id }
    existingSiblings.forEach {
      it.qaChecksStale = true
      translationService.save(it)
    }

    // Expand the targets to project languages - includes languages for which there is no translation yet
    val allLanguageIds = languageService.getProjectLanguages(event.key.project.id).map { it.id }
    return allLanguageIds
      .filter { it != baseLanguage.id && it !in excludedLanguageIds }
      .map { BatchTranslationTargetItem(keyId = event.key.id, languageId = it) }
  }
}
