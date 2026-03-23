package io.tolgee.ee.component.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.constants.Feature
import io.tolgee.events.OnTranslationsSet
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
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onTranslationsSet(event: OnTranslationsSet) {
    val project = event.key.project
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

    val directTranslationIds = event.translations.map { it.id }
    if (directTranslationIds.isEmpty()) return

    val siblingIds = getSiblingIdsForBaseTextChange(event, directTranslationIds.toSet())

    batchJobService.startJob(
      request = QaCheckRequest(translationIds = directTranslationIds + siblingIds),
      project = project,
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }

  private fun getSiblingIdsForBaseTextChange(
    event: OnTranslationsSet,
    excludedIds: Set<Long>,
  ): List<Long> {
    val baseLanguage = event.key.project.baseLanguage ?: return emptyList()

    val oldBaseValue = event.oldValues[baseLanguage.tag]
    val newBaseValue = event.translations.find { it.language.id == baseLanguage.id }?.text
    val baseTextUnchanged = oldBaseValue == newBaseValue

    if (baseTextUnchanged) return emptyList()

    val siblings =
      event.key.translations
        .filter { t ->
          t.language.id != baseLanguage.id &&
            t.id !in excludedIds
        }.distinctBy { it.id }

    siblings.forEach {
      it.qaChecksStale = true
      translationService.save(it)
    }

    return siblings.map { it.id }
  }
}
