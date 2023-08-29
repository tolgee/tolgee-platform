package io.tolgee.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.projectActivityView.ProjectActivityViewDataProvider
import io.tolgee.dtos.query_results.TranslationHistoryView
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityModifiedEntityRepository
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityExistsException
import javax.persistence.EntityManager

@Service
class ActivityService(
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val activityModifiedEntityRepository: ActivityModifiedEntityRepository
) : Logging {
  @Transactional
  fun storeActivityData(activityRevision: ActivityRevision, modifiedEntities: ModifiedEntitiesType) {
    val mergedActivityRevision = activityRevision.persist()
    mergedActivityRevision.persistedDescribingRelations()

    mergedActivityRevision.modifiedEntities = modifiedEntities.values.flatMap { it.values }.toMutableList()
    mergedActivityRevision.persistModifiedEntities()

    entityManager.flush()

    applicationContext.publishEvent(OnProjectActivityStoredEvent(this, mergedActivityRevision))
  }

  private fun ActivityRevision.persistModifiedEntities() {
    modifiedEntities.forEach { activityModifiedEntity ->
      try {
        entityManager.persist(activityModifiedEntity)
      } catch (e: EntityExistsException) {
        logger.debug("ModifiedEntity entity already exists in persistence context, skipping", e)
      }
    }
  }

  private fun ActivityRevision.persistedDescribingRelations() {
    @Suppress("UselessCallOnCollection")
    describingRelations.filterNotNull().forEach {
      entityManager.persist(it)
    }
  }

  private fun ActivityRevision.persist(): ActivityRevision {
    return if (id == 0L) {
      entityManager.persist(this)
      this
    } else {
      entityManager.getReference(ActivityRevision::class.java, id)
    }
  }

  @Transactional
  fun getProjectActivity(projectId: Long, pageable: Pageable): Page<ProjectActivityView> {
    return ProjectActivityViewDataProvider(
      applicationContext = applicationContext,
      projectId = projectId,
      pageable = pageable
    ).getProjectActivity()
  }

  @Transactional
  fun getTranslationHistory(translationId: Long, pageable: Pageable): Page<TranslationHistoryView> {
    return activityModifiedEntityRepository.getTranslationHistory(
      translationId = translationId,
      pageable = pageable,
      ignoredActivityTypes = listOf(ActivityType.TRANSLATION_COMMENT_ADD)
    )
  }
}
