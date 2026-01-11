package io.tolgee.activity.projectActivity

import io.tolgee.activity.data.ActivityType
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
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.context.ApplicationContext

class ActivityViewByRevisionsProvider(
  private val applicationContext: ApplicationContext,
  private val revisions: Collection<ActivityRevision>,
  /**
   * For Activities, which have onlyCountInList = true in io.tolgee.activity.data.ActivityType,
   * this parameter specifies what's the maximum per-entity modification to be included in the list.
   */
  private val onlyCountInListAbove: Int = 0,
) {
  val userAccountService: UserAccountService =
    applicationContext.getBean(UserAccountService::class.java)

  private val activityRevisionRepository: ActivityRevisionRepository =
    applicationContext.getBean(ActivityRevisionRepository::class.java)

  private val entityManager: EntityManager =
    applicationContext.getBean(EntityManager::class.java)

  private lateinit var authors: Map<Long, UserAccount>
  private lateinit var modifiedEntities: Map<Long, List<ModifiedEntityView>>
  private lateinit var revisionIds: MutableList<Long>
  private lateinit var counts: MutableMap<Long, MutableMap<String, Long>>
  private lateinit var allDataReturningActivityTypes: List<ActivityType>
  private lateinit var allRelationData: MutableMap<Long, MutableList<ActivityDescribingEntity>>
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
        params = params[revision.id],
      )
    }
  }

  private fun prepareData() {
    revisionIds = revisions.map { it.id }.toMutableList()

    allDataReturningActivityTypes = ActivityType.entries.filter { !it.onlyCountsInList }

    counts = getCounts()

    rawModifiedEntities = getModifiedEntitiesRaw()

    allRelationData = RelationDataProvider(entityManager, rawModifiedEntities).provide()

    entityExistences = getEntityExistences()

    modifiedEntities = this.getModifiedEntities()

    params = getParams()

    authors = getAuthors(revisions)
  }

  private fun getParams(): Map<Long, Any?> {
    val result = mutableMapOf<Long, Any?>()

    revisions
      .filter { it.type?.paramsProvider != null }
      .groupBy { it.type?.paramsProvider }
      .forEach { (providerClass, revisions) ->
        providerClass ?: return@forEach
        val revisionIds = revisions.map { it.id }
        applicationContext
          .getBean(providerClass.java)
          .provide(revisionIds)
          .forEach(result::put)
      }

    return result
  }

  private fun getCounts(): MutableMap<Long, MutableMap<String, Long>> {
    val allowedTypes = ActivityType.entries.filter { it.onlyCountsInList }
    val counts: MutableMap<Long, MutableMap<String, Long>> = mutableMapOf()
    activityRevisionRepository
      .getModifiedEntityTypeCounts(
        revisionIds = revisionIds,
        allowedTypes,
      ).forEach { (revisionId, entityClass, count) ->
        counts
          .computeIfAbsent(revisionId as Long) { mutableMapOf() }
          .computeIfAbsent(entityClass as String) { count as Long }
      }
    return counts
  }

  private fun getAuthors(revisions: Collection<ActivityRevision>) =
    userAccountService
      .getAllByIdsIncludingDeleted(
        revisions.mapNotNull { it.authorId }.toSet(),
      ).associateBy { it.id }

  private fun getModifiedEntities(): Map<Long, List<ModifiedEntityView>> {
    val factory = ModifiedEntityViewFactory(entityExistences, allRelationData)
    return rawModifiedEntities
      .groupBy { it.activityRevision.id }
      .mapValues { (_, modifiedEntities) ->
        modifiedEntities.map { modifiedEntity ->
          factory.create(modifiedEntity)
        }
      }
  }

  private fun getEntityExistences(): Map<Pair<String, Long>, Boolean> {
    return EntityExistenceProvider(applicationContext, rawModifiedEntities, allRelationData).provide()
  }

  private fun getModifiedEntitiesRaw(): List<ActivityModifiedEntity> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ActivityModifiedEntity::class.java)
    val root = query.from(ActivityModifiedEntity::class.java)
    val revision = root.join(ActivityModifiedEntity_.activityRevision)

    val filter = getClassesToExpandFilter(cb, revision, root)

    val whereConditions = mutableListOf<Predicate>()
    whereConditions.add(filter)
    whereConditions.add(revision.get(ActivityRevision_.id).`in`(revisionIds))
    ActivityType.entries.forEach {
      it.restrictEntitiesInList?.let { restrictEntitiesInList ->
        val restrictedEntityNames = restrictEntitiesInList.map { it.simpleName }
        whereConditions.add(
          cb.or(
            cb.notEqual(revision.get(ActivityRevision_.type), it),
            root.get(ActivityModifiedEntity_.entityClass).`in`(restrictedEntityNames),
          ),
        )
      }
    }

    query.where(cb.and(*whereConditions.toTypedArray()))
    return entityManager.createQuery(query).resultList
  }

  private fun getClassesToExpandFilter(
    cb: CriteriaBuilder,
    revision: Join<ActivityModifiedEntity, ActivityRevision>,
    root: Root<ActivityModifiedEntity>,
  ): Predicate {
    val toExpand = classesToExpand

    val expandFilters =
      toExpand.map {
        cb.and(
          cb.equal(revision.get(ActivityRevision_.id), it.key),
          root.get(ActivityModifiedEntity_.entityClass).`in`(it.value),
        )
      }

    val allActivityTypeFilter = revision.get(ActivityRevision_.type).`in`(allDataReturningActivityTypes)

    if (expandFilters.isEmpty()) {
      return allActivityTypeFilter
    }

    val filter =
      cb.or(
        cb.and(*expandFilters.toTypedArray()),
        allActivityTypeFilter,
      )

    return filter
  }

  private val classesToExpand: Map<Long, Set<String>>
    by lazy {
      counts
        .mapNotNull { (revisionId, counts) ->
          val allowedClasses =
            counts.entries
              .filter { it.value <= onlyCountInListAbove }
              .map { it.key }
              .toSet()
          if (allowedClasses.isEmpty()) {
            return@mapNotNull null
          }
          revisionId to allowedClasses
        }.toMap()
    }
}
