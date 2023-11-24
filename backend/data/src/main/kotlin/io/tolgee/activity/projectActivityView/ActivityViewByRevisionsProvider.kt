package io.tolgee.activity.projectActivityView

import io.sentry.Sentry
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.model.UserAccount
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityModifiedEntity_
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.activity.ActivityRevision_
import io.tolgee.model.views.activity.ModifiedEntityView
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.EntityUtil
import org.springframework.context.ApplicationContext
import javax.persistence.EntityManager
import javax.persistence.criteria.Predicate

class ActivityViewByRevisionsProvider(
  private val applicationContext: ApplicationContext,
  private val revisions: Collection<ActivityRevision>,
) {
  val userAccountService: UserAccountService =
    applicationContext.getBean(UserAccountService::class.java)

  private val activityRevisionRepository: ActivityRevisionRepository =
    applicationContext.getBean(ActivityRevisionRepository::class.java)

  private val entityManager: EntityManager =
    applicationContext.getBean(EntityManager::class.java)

  private val entityUtil: EntityUtil =
    applicationContext.getBean(EntityUtil::class.java)

  private lateinit var authors: Map<Long, UserAccount>
  private lateinit var modifiedEntities: Map<Long, List<ModifiedEntityView>>
  private lateinit var revisionIds: MutableList<Long>
  private lateinit var counts: MutableMap<Long, MutableMap<String, Long>>
  private lateinit var allDataReturningEventTypes: List<ActivityType>
  private lateinit var allRelationData: Map<Long, List<ActivityDescribingEntity>>
  private lateinit var rawModifiedEntities: List<ActivityModifiedEntity>
  private lateinit var entityExistences: Map<Pair<String, Long>, Boolean>
  private lateinit var params: Map<Long, Any?>

  fun get(): List<ProjectActivityView> {
    prepareData()

    return revisions.map { revision ->
      val author = authors[revision.authorId]
      ProjectActivityView(
        revisionId = revision.id,
        timestamp = revision.timestamp.time,
        type = revision.type ?: ActivityType.UNKNOWN,
        authorId = revision.authorId,
        authorUsername = author?.username,
        authorName = author?.name,
        authorAvatarHash = author?.avatarHash,
        authorDeleted = author?.deletedAt != null,
        meta = revision.meta,
        modifications = modifiedEntities[revision.id],
        counts = counts[revision.id],
        params = params[revision.id]
      )
    }
  }

  private fun prepareData() {
    revisionIds = revisions.map { it.id }.toMutableList()

    allDataReturningEventTypes = ActivityType.values().filter { !it.onlyCountsInList }

    allRelationData = getAllowedRevisionRelations(revisionIds, allDataReturningEventTypes)

    rawModifiedEntities = getModifiedEntitiesRaw()

    entityExistences = getEntityExistences()

    modifiedEntities = this.getModifiedEntities()

    params = getParams()

    authors = getAuthors(revisions)

    counts = getCounts()
  }

  private fun getParams(): Map<Long, Any?> {
    val result = mutableMapOf<Long, Any?>()

    revisions
      .filter { it.type?.paramsProvider != null }
      .groupBy { it.type?.paramsProvider }
      .forEach { (providerClass, revisions) ->
        providerClass ?: return@forEach
        val revisionIds = revisions.map { it.id }
        applicationContext.getBean(providerClass.java)
          .provide(revisionIds).forEach(result::put)
      }

    return result
  }

  private fun getCounts(): MutableMap<Long, MutableMap<String, Long>> {
    val allowedTypes = ActivityType.values().filter { it.onlyCountsInList }
    val counts: MutableMap<Long, MutableMap<String, Long>> = mutableMapOf()
    activityRevisionRepository.getModifiedEntityTypeCounts(
      revisionIds = revisionIds,
      allowedTypes
    ).forEach { (revisionId, entityClass, count) ->
      counts
        .computeIfAbsent(revisionId as Long) { mutableMapOf() }
        .computeIfAbsent(entityClass as String) { count as Long }
    }
    return counts
  }

  private fun getAuthors(revisions: Collection<ActivityRevision>) =
    userAccountService.getAllByIdsIncludingDeleted(
      revisions.mapNotNull { it.authorId }.toSet()
    ).associateBy { it.id }

  private fun getAllowedRevisionRelations(
    revisionIds: List<Long>,
    allowedTypes: Collection<ActivityType>
  ): Map<Long, List<ActivityDescribingEntity>> {
    return activityRevisionRepository.getRelationsForRevisions(revisionIds, allowedTypes)
      .groupBy { it.activityRevision.id }
  }

  private fun getModifiedEntities(): Map<Long, List<ModifiedEntityView>> {
    return rawModifiedEntities.map { modifiedEntity ->
      val relations = modifiedEntity.describingRelations
        ?.mapNotNull relationsOfEntityMap@{ relationEntry ->
          relationEntry.key to extractCompressedRef(
            relationEntry.value,
            allRelationData[modifiedEntity.activityRevision.id] ?: let {
              Sentry.captureException(
                IllegalStateException("No relation data for revision ${modifiedEntity.activityRevision.id}")
              )
              return@relationsOfEntityMap null
            }
          )
        }?.toMap()
      ModifiedEntityView(
        activityRevision = modifiedEntity.activityRevision,
        entityClass = modifiedEntity.entityClass,
        entityId = modifiedEntity.entityId,
        exists = entityExistences[modifiedEntity.entityClass to modifiedEntity.entityId],
        modifications = modifiedEntity.modifications,
        description = modifiedEntity.describingData,
        describingRelations = relations
      )
    }.groupBy { it.activityRevision.id }
  }

  private fun getEntityExistences(): Map<Pair<String, Long>, Boolean> {
    val modifiedEntityClassIdPairs = rawModifiedEntities.map { it.entityClass to it.entityId }
    val relationsClassIdPairs = allRelationData.flatMap { (_, data) -> data.map { it.entityClass to it.entityId } }
    val entities = (modifiedEntityClassIdPairs + relationsClassIdPairs).toHashSet()

    return entities
      .groupBy { (entityClass, _) -> entityClass }
      .mapNotNull { (entityClassName, classIdPairs) ->
        val entityClass = entityUtil.getRealEntityClass(entityClassName)
        val annotation = entityClass?.getAnnotation(ActivityReturnsExistence::class.java)
        if (annotation != null) {
          val cb = entityManager.criteriaBuilder
          val query = cb.createQuery(Long::class.java)
          val root = query.from(entityClass)
          val ids = classIdPairs.map { it.second }
          query.select(root.get("id"))
          query.where(root.get<Boolean?>("id").`in`(ids))
          val existingIds = entityManager.createQuery(query).resultList
          return@mapNotNull (entityClassName to ids.map { it to existingIds.contains(it) })
        }
        return@mapNotNull null
      }
      .flatMap { (entityClassName, existingIds) -> existingIds.map { (entityClassName to it.first) to it.second } }
      .toMap()
  }

  private fun getModifiedEntitiesRaw(): List<ActivityModifiedEntity> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ActivityModifiedEntity::class.java)
    val root = query.from(ActivityModifiedEntity::class.java)
    val revision = root.join(ActivityModifiedEntity_.activityRevision)

    val whereConditions = mutableListOf<Predicate>()
    whereConditions.add(revision.get(ActivityRevision_.type).`in`(allDataReturningEventTypes))
    whereConditions.add(revision.get(ActivityRevision_.id).`in`(revisionIds))
    ActivityType.values().forEach {
      it.restrictEntitiesInList?.let { restrictEntitiesInList ->
        val restrictedEntityNames = restrictEntitiesInList.map { it.simpleName }
        whereConditions.add(
          cb.or(
            cb.notEqual(revision.get(ActivityRevision_.type), it),
            root.get(ActivityModifiedEntity_.entityClass).`in`(restrictedEntityNames)
          )
        )
      }
    }

    query.where(cb.and(*whereConditions.toTypedArray()))
    return entityManager.createQuery(query).resultList
  }

  private fun extractCompressedRef(
    value: EntityDescriptionRef,
    describingEntities: List<ActivityDescribingEntity>
  ): ExistenceEntityDescription {
    val entity = describingEntities.find { it.entityClass == value.entityClass && it.entityId == value.entityId }

    val relations = entity?.describingRelations
      ?.map { it.key to extractCompressedRef(it.value, describingEntities) }
      ?.toMap()

    return ExistenceEntityDescription(
      entityClass = value.entityClass,
      entityId = value.entityId,
      exists = entityExistences[value.entityClass to value.entityId],
      data = entity?.data ?: mapOf(),
      relations = relations ?: mapOf()
    )
  }
}
