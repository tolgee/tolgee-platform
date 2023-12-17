package io.tolgee.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.projectActivityView.ProjectActivityViewByPageableProvider
import io.tolgee.activity.projectActivityView.ProjectActivityViewByRevisionProvider
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.query_results.TranslationHistoryView
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityModifiedEntityRepository
import io.tolgee.util.Logging
import io.tolgee.util.doInStatelessSession
import io.tolgee.util.flushAndClear
import jakarta.persistence.EntityManager
import org.hibernate.StatelessSession
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActivityService(
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val activityModifiedEntityRepository: ActivityModifiedEntityRepository,
  private val currentDateProvider: CurrentDateProvider
) : Logging {
  @Transactional
  fun storeActivityData(activityRevision: ActivityRevision, modifiedEntities: ModifiedEntitiesType) {
    // let's keep the persistent context small
    entityManager.flushAndClear()

    val mergedActivityRevision = entityManager.doInStatelessSession { statelessSession ->
      val mergedActivityRevision = statelessSession.persist(activityRevision)
      statelessSession.persistedDescribingRelations(mergedActivityRevision)
      mergedActivityRevision.modifiedEntities = statelessSession.persist(modifiedEntities)
      mergedActivityRevision
    }
    applicationContext.publishEvent(OnProjectActivityStoredEvent(this, mergedActivityRevision))
  }

  private fun StatelessSession.persist(modifiedEntities: ModifiedEntitiesType): MutableList<ActivityModifiedEntity> {
    val list = modifiedEntities.values.flatMap { it.values }.toMutableList()
    list.forEach { activityModifiedEntity ->
      this.insert(activityModifiedEntity)
    }
    return list
  }

  private fun StatelessSession.persistedDescribingRelations(activityRevision: ActivityRevision) {
    @Suppress("UselessCallOnCollection")
    activityRevision.describingRelations.filterNotNull().forEach {
      this.insert(it)
    }
  }

  private fun StatelessSession.persist(activityRevision: ActivityRevision): ActivityRevision {
    return if (activityRevision.id == 0L) {
      activityRevision.timestamp = currentDateProvider.date
      this.insert(activityRevision)
      activityRevision
    } else {
      entityManager.getReference(ActivityRevision::class.java, activityRevision.id)
    }
  }

  @Transactional
  fun getProjectActivity(projectId: Long, pageable: Pageable): Page<ProjectActivityView> {
    return ProjectActivityViewByPageableProvider(
      applicationContext = applicationContext,
      projectId = projectId,
      pageable = pageable
    ).get()
  }

  @Transactional
  fun getProjectActivity(revisionId: Long): ProjectActivityView? {
    return ProjectActivityViewByRevisionProvider(
      applicationContext = applicationContext,
      revisionId
    ).get()
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
