package io.tolgee.component.autoTranslation

import com.google.cloud.translate.Translation
import io.tolgee.activity.data.ActivityType
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.key.Key
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.AutoTranslationService
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import kotlin.properties.Delegates

/**
 * Utility class which handles auto translation.
 *
 * Separated from [AutoTranslationListener] to be able to cache some data in local
 * variables without need to pass the params to so many methods.
 */
class AutoTranslationEventHandler(
  private val event: OnProjectActivityStoredEvent,
  private val applicationContext: ApplicationContext,
) {
  var projectId by Delegates.notNull<Long>()

  fun handle() {
    projectId = event.activityRevision.projectId ?: return

    if (!shouldRunTheOperation()) {
      return
    }

    val keyIds = getKeyIdsToAutoTranslate()

    if (keyIds.isEmpty()) {
      return
    }

    autoTranslationService.autoTranslateViaBatchJob(
      projectId = projectId,
      keyIds = keyIds,
      isBatch = true,
      baseLanguageId = baseLanguageId ?: return,
      isHiddenJob = event.isLowVolumeActivity(),
    )
  }

  private fun shouldRunTheOperation(): Boolean {
    if (event.activityRevision.modifiedEntities.none { it.entityClass == Translation::class.simpleName }) {
      return false
    }

    val configs =
      autoTranslationService.getConfigs(
        entityManager.getReference(Project::class.java, projectId),
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

  private fun OnProjectActivityStoredEvent.isLowVolumeActivity() = activityRevision.type in LOW_VOLUME_ACTIVITIES

  private fun getKeyIdsToAutoTranslate(): List<Long> {
    return event.activityRevision.modifiedEntities.mapNotNull { modifiedEntity ->
      if (!modifiedEntity.isBaseTranslationTextChanged()) {
        return@mapNotNull null
      }
      getKeyId(modifiedEntity)
    }
  }

  private fun ActivityModifiedEntity.isBaseTranslationTextChanged(): Boolean {
    return this.isTranslation() && this.isBaseTranslation() && this.isTextChanged()
  }

  private fun getKeyId(modifiedEntity: ActivityModifiedEntity) =
    modifiedEntity.describingRelations
      ?.values
      ?.find { it.entityClass == Key::class.simpleName }
      ?.entityId

  private fun ActivityModifiedEntity.isTextChanged(): Boolean {
    val modification = this.modifications["text"] ?: return false
    return (modification.new as? String)?.isBlank() == false &&
      modification.old != modification.new
  }

  private fun ActivityModifiedEntity.isTranslation() = entityClass == Translation::class.simpleName

  private fun ActivityModifiedEntity.isBaseTranslation(): Boolean {
    return describingRelations
      ?.values
      ?.any { it.entityClass == Language::class.simpleName && it.entityId == baseLanguageId }
      ?: false
  }

  private val baseLanguageId: Long? by lazy {
    projectService.get(projectId).baseLanguage?.id
  }

  private val autoTranslationService: AutoTranslationService by lazy {
    applicationContext.getBean(AutoTranslationService::class.java)
  }

  private val projectService: ProjectService by lazy {
    applicationContext.getBean(ProjectService::class.java)
  }

  private val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  companion object {
    /**
     * import activities start visible batch job
     */
    private val IMPORT_ACTIVITIES = listOf(ActivityType.IMPORT)

    /**
     * Low volume activities start hidden batch job
     */
    private val LOW_VOLUME_ACTIVITIES =
      listOf(ActivityType.SET_TRANSLATIONS, ActivityType.CREATE_KEY, ActivityType.COMPLEX_EDIT)
  }
}
