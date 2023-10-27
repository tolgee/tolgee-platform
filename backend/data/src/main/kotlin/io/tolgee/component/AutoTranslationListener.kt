package io.tolgee.component

import com.google.cloud.translate.Translation
import io.tolgee.activity.data.ActivityType
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.key.Key
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.util.Logging
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class AutoTranslationListener(
  private val autoTranslationService: AutoTranslationService,
  private val projectService: ProjectService,
  private val entityManager: EntityManager,
) : Logging {

  companion object {
    /**
     * import activities start visible batch job
     */
    private val IMPORT_ACTIVITIES = listOf(ActivityType.IMPORT)

    /**
     * Low volume activities start hidden batch job
     */
    private val LOW_VOLUME_ACTIVITIES = listOf(ActivityType.SET_TRANSLATIONS, ActivityType.CREATE_KEY, ActivityType.COMPLEX_EDIT)
  }

  @Order(2)
  @EventListener
  fun onApplicationEvent(event: OnProjectActivityStoredEvent) {
    val projectId = event.activityRevision.projectId ?: return

    if (!shouldRunTheOperation(event, projectId)) {
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
        isHiddenJob = event.isLowVolumeActivity()
      )
    }
  }

  private fun shouldRunTheOperation(event: OnProjectActivityStoredEvent, projectId: Long): Boolean {
    val configs = autoTranslationService.getConfigs(
      entityManager.getReference(Project::class.java, projectId)
    )

    val usingPrimaryMtService = configs.any { it.usingPrimaryMtService }
    val usingTm = configs.any { it.usingTm }

    if (!usingPrimaryMtService && !usingTm) {
      return false
    }

    if (event.isLowVolumeActivity()) {
      return true
    }

    val hasEnabledForImport = configs.any { it.enableForImport }

    return hasEnabledForImport && event.activityRevision.type in IMPORT_ACTIVITIES
  }

  private fun OnProjectActivityStoredEvent.isLowVolumeActivity() =
    activityRevision.type in LOW_VOLUME_ACTIVITIES

  fun getKeyIdsToAutoTranslate(projectId: Long, modifiedEntities: MutableList<ActivityModifiedEntity>): List<Long> {
    return modifiedEntities.mapNotNull { modifiedEntity ->
      if (!modifiedEntity.isBaseTranslationTextChanged(projectId)) {
        return@mapNotNull null
      }
      getKeyId(modifiedEntity)
    }
  }

  private fun ActivityModifiedEntity.isBaseTranslationTextChanged(projectId: Long): Boolean {
    return this.isTranslation() && this.isBaseTranslation(projectId) && this.isTextChanged()
  }

  private fun getKeyId(modifiedEntity: ActivityModifiedEntity) =
    modifiedEntity.describingRelations?.values
      ?.find { it.entityClass == Key::class.simpleName }?.entityId

  private fun ActivityModifiedEntity.isTextChanged(): Boolean {
    val modification = this.modifications["text"] ?: return false
    return (modification.new as? String)?.isBlank() == false &&
      modification.old != modification.new
  }

  private fun ActivityModifiedEntity.isTranslation() =
    entityClass == Translation::class.simpleName

  private fun ActivityModifiedEntity.isBaseTranslation(projectId: Long): Boolean {
    val baseLanguageId = projectService.get(projectId).baseLanguage?.id ?: return false

    return describingRelations?.values
      ?.any { it.entityClass == Language::class.simpleName && it.entityId == baseLanguageId }
      ?: false
  }
}
