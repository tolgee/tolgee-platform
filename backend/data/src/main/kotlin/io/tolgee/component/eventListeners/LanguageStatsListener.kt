package io.tolgee.component.eventListeners

import io.tolgee.activity.ActivityService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import kotlin.reflect.KClass

@Component
class LanguageStatsListener(
  private var languageStatsService: LanguageStatsService,
  private val projectService: ProjectService,
  private val keyRepository: KeyRepository,
  private val translationRepository: TranslationRepository,
  private val activityService: ActivityService,
) {
  var bypass = false

  @TransactionalEventListener
  @Async
  fun onActivity(event: OnProjectActivityEvent) {
    if (bypass) return
    runSentryCatching {
      val projectId = event.activityRevision.projectId ?: return

      // exit when a project doesn't exist anymore
      projectService.findDto(projectId) ?: return

      val modifiedEntityClasses = event.modifiedEntities.keys.toSet()
      if (!affectsStats(modifiedEntityClasses)) return@runSentryCatching

      if (affectsGlobalStats(modifiedEntityClasses)) {
        languageStatsService.refreshLanguageStats(projectId)
        return@runSentryCatching
      }

      val branchIds = resolveBranches(event)

      if (branchIds.contains(null)) {
        languageStatsService.refreshLanguageStats(projectId)
        return@runSentryCatching
      }

      branchIds.forEach { branchId ->
        languageStatsService.refreshLanguageStats(projectId, branchId)
      }
    }
  }

  @EventListener(OnBatchJobFinalized::class)
  @Async
  fun onQaBatchJobFinalized(event: OnBatchJobFinalized) {
    if (bypass) return
    if (event.job.type != BatchJobType.QA_CHECK) return
    val projectId = event.job.projectId ?: return
    runSentryCatching {
      projectService.findDto(projectId) ?: return@runSentryCatching

      val branchIds = activityService.findDistinctBranchIdsByRevisionId(event.activityRevisionId)
      if (branchIds.isEmpty()) return@runSentryCatching

      branchIds.forEach { branchId ->
        languageStatsService.refreshLanguageStats(projectId, branchId)
      }
    }
  }

  private fun affectsStats(classes: Set<KClass<*>>): Boolean {
    return classes.contains(Language::class) ||
      classes.contains(Translation::class) ||
      classes.contains(Key::class) ||
      classes.contains(Project::class)
  }

  private fun affectsGlobalStats(classes: Set<KClass<*>>): Boolean {
    return classes.contains(Language::class) || classes.contains(Project::class)
  }

  private fun resolveBranches(event: OnProjectActivityEvent): Set<Long?> {
    val branchIds = mutableSetOf<Long?>()
    collectBranchIdsForKeys(event, branchIds)
    collectBranchIdsForTranslations(event, branchIds)
    return branchIds
  }

  private fun collectBranchIdsForKeys(
    event: OnProjectActivityEvent,
    branchIds: MutableSet<Long?>,
  ) {
    val keyIds = event.modifiedEntities[Key::class]?.keys.orEmpty()
    if (keyIds.isEmpty()) return

    keyIds.chunked(IN_CLAUSE_BATCH_SIZE).forEach { chunk ->
      branchIds.addAll(keyRepository.getBranchIdsByIds(chunk))
    }
  }

  private fun collectBranchIdsForTranslations(
    event: OnProjectActivityEvent,
    branchIds: MutableSet<Long?>,
  ) {
    val translationIds = event.modifiedEntities[Translation::class]?.keys.orEmpty()
    if (translationIds.isEmpty()) return

    translationIds.chunked(IN_CLAUSE_BATCH_SIZE).forEach { chunk ->
      branchIds.addAll(translationRepository.getBranchIdsByIds(chunk))
    }
  }

  companion object {
    private const val IN_CLAUSE_BATCH_SIZE = 30_000
  }
}
