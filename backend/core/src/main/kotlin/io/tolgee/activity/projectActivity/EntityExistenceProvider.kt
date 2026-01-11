package io.tolgee.activity.projectActivity

import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.util.EntityUtil
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext

class EntityExistenceProvider(
  private val applicationContext: ApplicationContext,
  private val rawModifiedEntities: Iterable<ActivityModifiedEntity>,
  private val allRelationData: MutableMap<Long, MutableList<ActivityDescribingEntity>>,
) {
  private val entityManager: EntityManager =
    applicationContext.getBean(EntityManager::class.java)

  private val entityUtil: EntityUtil =
    applicationContext.getBean(EntityUtil::class.java)

  fun provide(): Map<Pair<String, Long>, Boolean> {
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
      }.flatMap { (entityClassName, existingIds) -> existingIds.map { (entityClassName to it.first) to it.second } }
      .toMap()
  }
}
