package io.tolgee.activity

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.RevisionType
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
) : Logging {
  @Transactional
  fun storeActivityData(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ) {
    if (!activityRevision.shouldSaveWithoutModification() && modifiedEntities.isEmpty()) {
      return
    }

    // let's keep the persistent context small
    entityManager.flushAndClear()

    val mergedActivityRevision = persistActivityRevision(activityRevision)

    persistedDescribingRelations(mergedActivityRevision)
    mergedActivityRevision.modifiedEntities = persistModifiedEntities(modifiedEntities)
    applicationContext.publishEvent(OnProjectActivityStoredEvent(this, mergedActivityRevision))
  }

  private fun persistModifiedEntities(modifiedEntities: ModifiedEntitiesType): MutableList<ActivityModifiedEntity> {
    val list = modifiedEntities.values.flatMap { it.values }.toMutableList()
    jdbcTemplate.batchUpdate(
      "INSERT INTO activity_modified_entity " +
        "(entity_class, entity_id, describing_data, " +
        "describing_relations, modifications, revision_type, activity_revision_id) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)",
      list,
      100,
    ) { ps, entity ->
      ps.setString(1, entity.entityClass)
      ps.setLong(2, entity.entityId)
      ps.setObject(3, getJsonbObject(entity.describingData))
      ps.setObject(4, getJsonbObject(entity.describingRelations))
      ps.setObject(5, getJsonbObject(entity.modifications))
      ps.setInt(6, RevisionType.values().indexOf(entity.revisionType))
      ps.setLong(7, entity.activityRevision.id)
    }

    return list
  }

  private fun persistedDescribingRelations(activityRevision: ActivityRevision) {
    jdbcTemplate.batchUpdate(
      "INSERT INTO activity_describing_entity " +
        "(entity_class, entity_id, data, describing_relations, activity_revision_id) " +
        "VALUES (?, ?, ?, ?, ?)",
      activityRevision.describingRelations,
      100,
    ) { ps, entity ->
      ps.setString(1, entity.entityClass)
      ps.setLong(2, entity.entityId)
      ps.setObject(3, getJsonbObject(entity.data))
      ps.setObject(4, getJsonbObject(entity.describingRelations))
      ps.setLong(5, activityRevision.id)
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
  fun findProjectActivity(
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
  fun findProjectActivity(revisionId: Long): ProjectActivityView? {
    return ProjectActivityViewByRevisionProvider(
      applicationContext = applicationContext,
      revisionId = revisionId,
    ).get()
  }

  @Transactional
  fun findProjectActivity(
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

  private fun ActivityRevision.shouldSaveWithoutModification(): Boolean {
    val type = this.type ?: return true
    return type.saveWithoutModification
  }
}
