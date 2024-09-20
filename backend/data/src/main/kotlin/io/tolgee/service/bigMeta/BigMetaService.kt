package io.tolgee.service.bigMeta

import io.tolgee.Metrics
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.BigMetaDto
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.dtos.queryResults.KeyIdFindResult
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.repository.KeysDistanceRepository
import io.tolgee.util.Logging
import io.tolgee.util.equalNullable
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.runSentryCatching
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

@Service
class BigMetaService(
  private val keysDistanceRepository: KeysDistanceRepository,
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager,
  private val jdbcTemplate: JdbcTemplate,
  private val currentDateProvider: CurrentDateProvider,
  private val metrics: Metrics,
) : Logging {
  companion object {
    const val MAX_DISTANCE_SCORE = 10000L
    const val MAX_POINTS = 2000L
    const val MAX_ORDER_DISTANCE = 20
  }

  fun saveKeyDistance(keysDistance: KeysDistance): KeysDistance {
    return keysDistanceRepository.save(keysDistance)
  }

  @Transactional
  fun store(
    data: BigMetaDto,
    project: Project,
  ) {
    store(data.relatedKeysInOrder, project)
  }

  @Transactional
  fun store(
    relatedKeysInOrder: MutableList<RelatedKeyDto>?,
    project: Project,
  ) {
    if (relatedKeysInOrder.isNullOrEmpty()) {
      return
    }

    val util =
      metrics.bigMetaNewDistancesComputeTimer.recordCallable {
        KeysDistanceUtil(relatedKeysInOrder, project, this)
      }!!

    metrics.bigMetaStoringTimer.recordCallable {
      insertNewDistances(util.newDistances)
    }
  }

  private fun insertNewDistances(toInsert: List<KeysDistanceDto>) {
    val timestamp = Timestamp(currentDateProvider.date.time)
    jdbcTemplate.batchUpdate(
      """
      insert into keys_distance (key1id, key2id, score, hits, created_at, updated_at, project_id) 
      values (?, ?, ?, ?, ?, ?, ?)
      on conflict (key1id, key2id) do update set score = excluded.score, hits = excluded.hits, updated_at = ?
      """,
      toInsert,
      10000,
    ) { ps, dto ->
      ps.setLong(1, dto.key1Id)
      ps.setLong(2, dto.key2Id)
      ps.setLong(3, dto.score)
      ps.setLong(4, dto.hits)
      ps.setTimestamp(5, timestamp)
      ps.setTimestamp(6, timestamp)
      ps.setLong(7, dto.projectId)
      ps.setTimestamp(8, timestamp)
    }
  }

  fun getKeyIdsForItems(
    relatedKeysInOrder: List<RelatedKeyDto>,
    projectId: Long,
  ): List<KeyIdFindResult> {
    // we need to chunk it to avoid stack overflow
    return relatedKeysInOrder.chunked(1000)
      .flatMap { relatedKeysInOrderChunk ->
        val query = getKeyIdsForItemsQuery(relatedKeysInOrderChunk, projectId)
        entityManager.createQuery(query).resultList
      }
  }

  @Transactional
  fun findExistingKeyDistances(
    keys: List<KeyIdFindResult>,
    project: Project,
  ): Set<KeysDistanceDto> {
    val keyIds = keys.map { it.id }
    return findExistingKeysDistancesDtosByIds(keyIds)
  }

  @Transactional
  fun findExistingKeysDistancesDtosByIds(keyIds: List<Long>): Set<KeysDistanceDto> {
    return entityManager.createQuery(
      """
       select new io.tolgee.service.bigMeta.KeysDistanceDto(kd.key1Id, kd.key2Id, kd.score, kd.project.id, kd.hits) from KeysDistance kd
        where kd.key1Id in (
            select kd2.key1Id from KeysDistance kd2 where kd2.key1Id in :data or kd2.key2Id in :data
        ) or kd.key2Id in (
            select kd3.key2Id from KeysDistance kd3 where kd3.key1Id in :data or kd3.key2Id in :data
        )
    """,
      KeysDistanceDto::class.java,
    )
      .setParameter("data", keyIds)
      .resultList.toSet()
  }

  fun get(id: Long): KeysDistance {
    return find(id) ?: throw NotFoundException()
  }

  fun find(id: Long): KeysDistance? {
    return this.keysDistanceRepository.findById(id).orElse(null)
  }

  fun getCloseKeyIds(keyId: Long): List<Long> = this.keysDistanceRepository.getCloseKeys(keyId)

  fun getCloseKeysWithBaseTranslation(
    keyId: Long,
    projectId: Long,
  ) = this.keysDistanceRepository.getCloseKeysWithBaseTranslation(keyId, projectId)

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
      """,
        ).setParameter("ids", ids).executeUpdate()
      }
    }
  }

  private fun getKeyIdsForItemsQuery(
    relatedKeysInOrderChunk: List<RelatedKeyDto>,
    projectId: Long,
  ): CriteriaQuery<KeyIdFindResult>? {
    val cb: CriteriaBuilder = entityManager.criteriaBuilder
    val query = cb.createQuery(KeyIdFindResult::class.java)
    val root = query.from(Key::class.java)
    val namespace = root.join(Key_.namespace, JoinType.LEFT)
    val predicates =
      relatedKeysInOrderChunk.map { key ->
        cb.and(
          cb.equal(root.get(Key_.name), key.keyName),
          cb.equalNullable(namespace.get(Namespace_.name), key.namespace),
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

  fun deleteAllByProjectId(id: Long) {
    keysDistanceRepository.deleteAllByProjectId(id)
  }
}
