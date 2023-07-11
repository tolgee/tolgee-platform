package io.tolgee.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.projectActivityView.ProjectActivityViewDataProvider
import io.tolgee.dtos.query_results.TranslationHistoryView
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityModifiedEntityRepository
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class ActivityService(
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val activityModifiedEntityRepository: ActivityModifiedEntityRepository
) {
  @Transactional
  fun storeActivityData(activityRevision: ActivityRevision, modifiedEntities: ModifiedEntitiesType) {
    activityRevision.modifiedEntities = modifiedEntities.values.flatMap { it.values }.toMutableList()

    entityManager.persist(activityRevision)
    activityRevision.describingRelations.forEach {
      entityManager.persist(it)
    }
    activityRevision.modifiedEntities.forEach { activityModifiedEntity ->
      entityManager.persist(activityModifiedEntity)
    }
    entityManager.flush()
    activityRevision.afterFlush?.invoke()
    applicationContext.publishEvent(OnProjectActivityStoredEvent(this, activityRevision))
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
