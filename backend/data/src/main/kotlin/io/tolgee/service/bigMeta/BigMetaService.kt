package io.tolgee.service.bigMeta

import io.tolgee.dtos.BigMetaDto
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.dtos.query_results.KeyIdFindResult
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.repository.KeysDistanceRepository
import io.tolgee.util.equalNullable
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.JoinType

@Service
class BigMetaService(
  private val keysDistanceRepository: KeysDistanceRepository,
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager
) {
  companion object {
    const val MAX_DISTANCE_SCORE = 10000L
    const val MAX_POINTS = 2000L
    const val MAX_ORDER_DISTANCE = 20
  }

  fun saveKeyDistance(keysDistance: KeysDistance): KeysDistance {
    return keysDistanceRepository.save(keysDistance)
  }

  @Transactional
  fun store(data: BigMetaDto, project: Project) {
    storeRelatedKeysInOrder(data.relatedKeysInOrder, project)
  }

  @Transactional
  fun storeRelatedKeysInOrder(
    relatedKeysInOrder: MutableList<RelatedKeyDto>,
    project: Project
  ) {
    val distances = KeysDistanceUtil(relatedKeysInOrder, project, this)
      .newDistances
    keysDistanceRepository.saveAll(distances)
  }

  fun getKeyIdsForItems(
    relatedKeysInOrder: List<RelatedKeyDto>,
    projectId: Long
  ): List<KeyIdFindResult> {
    // we need to chunk it to avoid stack overflow
    return relatedKeysInOrder.chunked(1000)
      .flatMap { relatedKeysInOrderChunk ->
        val query = getKeyIdsForItemsQuery(relatedKeysInOrderChunk, projectId)
        entityManager.createQuery(query).resultList
      }
  }

  @Transactional
  fun findExistingKeyDistances(keys: List<KeyIdFindResult>, project: Project): List<KeysDistance> {
    val keyIds = keys.map { it.id }
    return findExistingKeysDistancesByIds(keyIds)
  }

  @Transactional
  fun findExistingKeysDistancesByIds(keyIds: List<Long>): List<KeysDistance> {
    val directIds = mutableSetOf<Long>()
    keysDistanceRepository.findForKeyIds(keyIds).forEach {
      directIds.add(it.key1Id)
      directIds.add(it.key2Id)
    }
    return keysDistanceRepository.findForKeyIds(directIds)
  }

  fun get(id: Long): KeysDistance {
    return find(id) ?: throw NotFoundException()
  }

  fun find(id: Long): KeysDistance? {
    return this.keysDistanceRepository.findById(id).orElse(null)
  }

  fun getCloseKeyIds(keyId: Long): List<Long> {
    return this.keysDistanceRepository.getCloseKeys(keyId)
      .flatMap { pair ->
        pair.toList()
          .filter { it != keyId }
      }
  }

  @EventListener(OnProjectActivityEvent::class)
  @Async
  fun onKeyDeleted(event: OnProjectActivityEvent) {
    runSentryCatching {
      val ids =
        event.modifiedEntities[Key::class]?.values?.filter { it.revisionType.isDel() }
          ?.map { it.entityId }

      if (ids.isNullOrEmpty()) {
        return
      }

      executeInNewTransaction(transactionManager) {
        entityManager.createQuery(
          """
      delete from KeysDistance kd 
      where kd.key1Id in :ids or kd.key2Id in :ids
      """
        ).setParameter("ids", ids).executeUpdate()
      }
    }
  }

  private fun getKeyIdsForItemsQuery(
    relatedKeysInOrderChunk: List<RelatedKeyDto>,
    projectId: Long
  ): CriteriaQuery<KeyIdFindResult>? {
    val cb: CriteriaBuilder = entityManager.criteriaBuilder
    val query = cb.createQuery(KeyIdFindResult::class.java)
    val root = query.from(Key::class.java)
    val namespace = root.join(Key_.namespace, JoinType.LEFT)
    val predicates = relatedKeysInOrderChunk.map { key ->
      cb.and(
        cb.equal(root.get(Key_.name), key.keyName),
        cb.equalNullable(namespace.get(Namespace_.name), key.namespace)
      )
    }
    val keyPredicates = cb.or(*predicates.toTypedArray())
    query.where(cb.and(keyPredicates, cb.equal(root.get(Key_.project).get(Project_.id), projectId)))
    query.multiselect(
      root.get(Key_.id).alias("id"),
      namespace.get(Namespace_.name).alias("namespace"),
      root.get(Key_.name).alias("name"),
    )
    return query
  }
}
