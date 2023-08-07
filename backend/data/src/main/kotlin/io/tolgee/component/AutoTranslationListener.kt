package io.tolgee.component

import com.google.cloud.translate.Translation
import io.tolgee.activity.data.ActivityType
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Language
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.key.Key
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.util.Logging
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class AutoTranslationListener(
  private val autoTranslationService: AutoTranslationService,
  private val projectService: ProjectService,
) : Logging {

  companion object {
    private val ACTIVITIES_FOR_VISIBLE_BATCH_JOB = listOf(ActivityType.IMPORT)
    private val ACTIVITIES_FOR_HIDDEN_JOB = listOf(ActivityType.SET_TRANSLATIONS, ActivityType.CREATE_KEY)
    private val ALLOWED_ACTIVITIES = ACTIVITIES_FOR_VISIBLE_BATCH_JOB + ACTIVITIES_FOR_HIDDEN_JOB
  }

  @Order(2)
  @EventListener
  fun onApplicationEvent(event: OnProjectActivityStoredEvent) {
    val projectId = event.activityRevision.projectId ?: return
    if (event.activityRevision.type !in ALLOWED_ACTIVITIES) {
      return
    }

    val keyIds = getKeyIdsToAutoTranslate(projectId, event.activityRevision.modifiedEntities)

    if (keyIds.isEmpty()) {
      return
    }

    if (event.activityRevision.modifiedEntities.any { it.entityClass == Translation::class.simpleName }) {
      autoTranslationService.autoTranslateViaBatchJob(
        projectId = projectId,
        keyIds = keyIds,
        isBatch = true,
        isHiddenJob = ACTIVITIES_FOR_HIDDEN_JOB.contains(event.activityRevision.type)
      )
    }
  }

  fun getKeyIdsToAutoTranslate(projectId: Long, modifiedEntities: MutableList<ActivityModifiedEntity>): List<Long> {
    val baseLanguageId = projectService.get(projectId).baseLanguage?.id ?: return listOf()
    return modifiedEntities.mapNotNull { modifiedEntity ->
      if (modifiedEntity.entityClass != Translation::class.simpleName) {
        return@mapNotNull null
      }

      val isBaseLanguageTranslation = modifiedEntity.describingRelations?.values
        ?.any { it.entityClass == Language::class.simpleName && it.entityId == baseLanguageId }
        ?: return@mapNotNull null

      if (!isBaseLanguageTranslation) {
        return@mapNotNull null
      }

      val modification = modifiedEntity.modifications["text"] ?: return@mapNotNull null
      val isBaseChanged = (modification.new as? String)?.isBlank() == false &&
        modification.old != modification.new

      if (!isBaseChanged) {
        return@mapNotNull null
      }

      modifiedEntity.describingRelations?.values
        ?.find { it.entityClass == Key::class.simpleName }?.entityId
    }
  }
}
