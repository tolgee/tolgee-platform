package io.tolgee.service.key

import io.tolgee.dtos.request.ComplexTagKeysRequest
import io.tolgee.dtos.request.KeyId
import io.tolgee.model.Project_
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.Tag
import io.tolgee.util.equalNullable
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import org.springframework.context.ApplicationContext

class ComplexTagOperationKeyProvider(
  private val projectId: Long,
  private val request: ComplexTagKeysRequest,
  private val applicationContext: ApplicationContext,
) {
  private val cb: CriteriaBuilder = entityManager.criteriaBuilder
  private val query = cb.createQuery(Key::class.java)
  private val root = query.from(Key::class.java)
  private val namespace = root.join(Key_.namespace, JoinType.LEFT)
  private val keyMeta = root.fetch(Key_.keyMeta, JoinType.LEFT)

  val filtered: List<Key> by lazy {
    @Suppress("UNCHECKED_CAST")
    (keyMeta as Join<Key, KeyMeta>).fetch(KeyMeta_.tags, JoinType.LEFT) as Join<KeyMeta, Tag>

    val conditions = getBaseConditions()

    request.filterKeys
      ?.map {
        getKeyCondition(it)
      }?.let {
        conditions.add(cb.or(*it.toTypedArray()))
      }

    request.filterKeysNot
      ?.map {
        getKeyCondition(it)
      }?.let {
        conditions.add(cb.not(cb.or(*it.toTypedArray())))
      }

    conditions.add(root.get(Key_.id).`in`(filteredByTagFiltersKeyIds))

    query.where(cb.and(*conditions.toTypedArray()))
    entityManager.createQuery(query).resultList
  }

  val rest: List<Key> by lazy {
    val returnedIds = filtered.map { it.id }
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Key::class.java)
    val root = query.from(Key::class.java)
    val keyMeta = root.fetch(Key_.keyMeta, JoinType.LEFT)
    @Suppress("UNCHECKED_CAST")
    (keyMeta as Join<Key, KeyMeta>).fetch(KeyMeta_.tags, JoinType.LEFT) as Join<KeyMeta, Tag>
    entityManager
      .createQuery(
        query
          .select(root)
          .where(
            cb.and(
              cb.equal(root.get(Key_.project).get(Project_.id), projectId),
              cb.notIn(root.get(Key_.id), returnedIds),
            ),
          ),
      ).resultList
  }

  private fun CriteriaBuilder.notIn(
    path: Path<Long>,
    collection: Collection<*>,
  ): Predicate {
    if (collection.isEmpty()) {
      return this.and()
    }
    return this.not(path.`in`(collection))
  }

  private fun getKeyCondition(key: KeyId): Predicate? {
    if (key.id != null) {
      return cb.equal(root.get(Key_.id), key.id)
    }

    return cb.and(
      cb.equal(root.get(Key_.name), key.name),
      cb.equalNullable(namespace.get(Namespace_.name), key.namespace),
    )
  }

  private fun getBaseConditions() =
    mutableListOf(
      cb.equal(root.get(Key_.project).get(Project_.id), projectId),
    )

  /**
   * It's extremely and inconvenient hard to write criteria query for this, so we will first get all the key ids
   * with specific tags and then use the key ids for further filtering
   */
  private val filteredByTagFiltersKeyIds by lazy {
    taggedKeyIdsProvider.getFilteredKeys(projectId, request.filterTag, request.filterTagNot)
  }

  private val entityManager: EntityManager
    get() = applicationContext.getBean(EntityManager::class.java)

  private val taggedKeyIdsProvider: TaggedKeyIdsProvider
    get() = applicationContext.getBean(TaggedKeyIdsProvider::class.java)
}
