package io.tolgee.activity

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
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
import org.postgresql.util.PGobject
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
  private val currentDateProvider: CurrentDateProvider,
  private val objectMapper: ObjectMapper
) : Logging {
  @Transactional
  fun storeActivityData(activityRevision: ActivityRevision, modifiedEntities: ModifiedEntitiesType) {
    // let's keep the persistent context small
    entityManager.flushAndClear()

    val mergedActivityRevision = entityManager.doInStatelessSession { statelessSession ->
      val mergedActivityRevision = statelessSession.persistAcitivyRevision(activityRevision)
      statelessSession.persistedDescribingRelations(mergedActivityRevision)
      mergedActivityRevision.modifiedEntities = statelessSession.persistModifiedEntities(modifiedEntities)
      mergedActivityRevision
    }
    applicationContext.publishEvent(OnProjectActivityStoredEvent(this, mergedActivityRevision))
  }

  private fun StatelessSession.persistModifiedEntities(modifiedEntities: ModifiedEntitiesType): MutableList<ActivityModifiedEntity> {
    val list = modifiedEntities.values.flatMap { it.values }.toMutableList()

    this.doWork { connection ->
      val describingRelationQuery = "INSERT INTO activity_modified_entity " +
        "(entity_class, entity_id, describing_data, " +
        "describing_relations, modifications, revision_type, activity_revision_id) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)"
      val preparedStatement = connection.prepareStatement(describingRelationQuery)
      list.forEach { entity ->
        preparedStatement.setString(1, entity.entityClass)
        preparedStatement.setLong(2, entity.entityId)
        preparedStatement.setObject(3, getJsonbObject(entity.describingData))
        preparedStatement.setObject(4, getJsonbObject(entity.describingRelations))
        preparedStatement.setObject(5, getJsonbObject(entity.modifications))
        preparedStatement.setInt(6, RevisionType.values().indexOf(entity.revisionType))
        preparedStatement.setLong(7, entity.activityRevision.id)
        preparedStatement.addBatch()
      }
      preparedStatement.executeBatch()
    }
    return list
  }


  private fun StatelessSession.persistedDescribingRelations(activityRevision: ActivityRevision) {
    this.doWork { connection ->
      val describingRelationQuery = "INSERT INTO activity_describing_entity " +
        "(entity_class, entity_id, data, describing_relations, activity_revision_id) " +
        "VALUES (?, ?, ?, ?, ?)"
      val preparedStatement = connection.prepareStatement(describingRelationQuery)
      activityRevision.describingRelations.forEach { entity ->
        preparedStatement.setString(1, entity.entityClass)
        preparedStatement.setLong(2, entity.entityId)
        preparedStatement.setObject(3, getJsonbObject(entity.data))
        preparedStatement.setObject(4, getJsonbObject(entity.describingRelations))
        preparedStatement.setLong(5, activityRevision.id)
        preparedStatement.addBatch()
      }
      preparedStatement.executeBatch()
    }
  }

  private fun getJsonbObject(data: Any?): PGobject {
    val pgObject = PGobject()
    pgObject.type = "jsonb"
    pgObject.value = objectMapper.writeValueAsString(data)
    return pgObject
  }

  private fun StatelessSession.persistAcitivyRevision(activityRevision: ActivityRevision): ActivityRevision {
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
