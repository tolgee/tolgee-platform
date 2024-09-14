package io.tolgee.activity

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
import io.tolgee.activity.groups.ActivityGroupService
import io.tolgee.activity.projectActivity.ModificationsByRevisionsProvider
import io.tolgee.activity.projectActivity.ProjectActivityViewByPageableProvider
import io.tolgee.activity.projectActivity.ProjectActivityViewByRevisionProvider
import io.tolgee.dtos.queryResults.TranslationHistoryView
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ModifiedEntityView
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityModifiedEntityRepository
import io.tolgee.util.Logging
import io.tolgee.util.flushAndClear
import jakarta.persistence.EntityManager
import org.postgresql.util.PGobject
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActivityService(
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val activityModifiedEntityRepository: ActivityModifiedEntityRepository,
  private val objectMapper: ObjectMapper,
  private val jdbcTemplate: JdbcTemplate,
  private val activityGroupService: ActivityGroupService,
  private val describers: List<ActivityAdditionalDescriber>,
) : Logging {
  @Transactional
  fun storeActivityData(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ) {
    // let's keep the persistent context small
    entityManager.flushAndClear()

    runAdditionalDescribers(activityRevision, modifiedEntities)

    val mergedActivityRevision = persistActivityRevision(activityRevision)
    persistedDescribingRelations(mergedActivityRevision)
    mergedActivityRevision.modifiedEntities = persistModifiedEntities(modifiedEntities)

    activityGroupService.addToGroup(activityRevision, modifiedEntities)

    applicationContext.publishEvent(OnProjectActivityStoredEvent(this, mergedActivityRevision))
  }

  private fun runAdditionalDescribers(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ) {
    describers.forEach {
      it.describe(activityRevision, modifiedEntities)
    }
  }

  private fun persistModifiedEntities(modifiedEntities: ModifiedEntitiesType): MutableList<ActivityModifiedEntity> {
    val list = modifiedEntities.values.flatMap { it.entries }.toMutableList()
    jdbcTemplate.batchUpdate(
      "INSERT INTO activity_modified_entity " +
        "(entity_class, entity_id, describing_data, " +
        "describing_relations, modifications, revision_type, activity_revision_id, additional_description) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
      list,
      100,
    ) { ps, (entityInstance, modifiedEntity) ->
      // the entity id can be null in some cases (probably when the id it's not allocated in batch)
      val entityId = if (modifiedEntity.entityId == 0L) entityInstance.id else modifiedEntity.entityId
      ps.setString(1, modifiedEntity.entityClass)
      ps.setLong(2, entityId)
      ps.setObject(3, getJsonbObject(modifiedEntity.describingData))
      ps.setObject(4, getJsonbObject(modifiedEntity.describingRelations))
      ps.setObject(5, getJsonbObject(modifiedEntity.modifications))
      ps.setInt(6, RevisionType.entries.indexOf(modifiedEntity.revisionType))
      ps.setLong(7, modifiedEntity.activityRevision.id)
      ps.setObject(8, getJsonbObject(modifiedEntity.additionalDescription))
    }

    return list.map { it.value }.toMutableList()
  }

  private fun persistedDescribingRelations(activityRevision: ActivityRevision) {
    jdbcTemplate.batchUpdate(
      "INSERT INTO activity_describing_entity " +
        "(entity_class, entity_id, data, describing_relations, activity_revision_id, additional_description) " +
        "VALUES (?, ?, ?, ?, ?, ?)",
      activityRevision.describingRelations,
      100,
    ) { ps, entity ->
      ps.setString(1, entity.entityClass)
      ps.setLong(2, entity.entityId)
      ps.setObject(3, getJsonbObject(entity.data))
      ps.setObject(4, getJsonbObject(entity.describingRelations))
      ps.setLong(5, activityRevision.id)
      ps.setObject(6, getJsonbObject(entity.additionalDescription))
    }
  }

  private fun getJsonbObject(data: Any?): PGobject {
    val pgObject = PGobject()
    pgObject.type = "jsonb"
    pgObject.value = objectMapper.writeValueAsString(data)
    return pgObject
  }

  private fun persistActivityRevision(activityRevision: ActivityRevision): ActivityRevision {
    return if (activityRevision.id == 0L) {
      entityManager.persist(activityRevision)
      entityManager.flushAndClear()
      activityRevision
    } else {
      entityManager.getReference(ActivityRevision::class.java, activityRevision.id)
    }
  }

  @Transactional
  fun getProjectActivity(
    projectId: Long,
    pageable: Pageable,
  ): Page<ProjectActivityView> {
    return ProjectActivityViewByPageableProvider(
      applicationContext = applicationContext,
      projectId = projectId,
      pageable = pageable,
    ).get()
  }

  @Transactional
  fun getProjectActivity(
    projectId: Long,
    revisionId: Long,
  ): ProjectActivityView? {
    return ProjectActivityViewByRevisionProvider(
      applicationContext = applicationContext,
      revisionId = revisionId,
      projectId = projectId,
    ).get()
  }

  @Transactional
  fun getTranslationHistory(
    translationId: Long,
    pageable: Pageable,
  ): Page<TranslationHistoryView> {
    return activityModifiedEntityRepository.getTranslationHistory(
      translationId = translationId,
      pageable = pageable,
      ignoredActivityTypes = listOf(ActivityType.TRANSLATION_COMMENT_ADD),
    )
  }

  fun getRevisionModifications(
    projectId: Long,
    revisionId: Long,
    pageable: Pageable,
    filterEntityClass: List<String>?,
  ): Page<ModifiedEntityView> {
    val provider =
      ModificationsByRevisionsProvider(applicationContext, projectId, listOf(revisionId), pageable, filterEntityClass)
    return provider.get()
  }
}
