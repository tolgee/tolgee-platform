package io.tolgee.service.bigMeta

import com.google.common.primitives.Longs
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.model.Project
import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.util.Logging
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class KeysDistanceUtil(
  private val relatedKeysInOrder: MutableList<RelatedKeyDto>,
  private val project: Project,
  private val bigMetaService: BigMetaService
) : Logging {
  val newDistances by lazy {
    increaseRelevant()
    decreaseOthers()
    distances.values
  }

  private fun increaseRelevant() {
    relatedKeysInOrder.forEachIndexed forEach1@{ index1, item1 ->
      val key1Id = keyIdMap[item1.namespace to item1.keyName] ?: return@forEach1
      relatedKeysInOrder.forEachIndexed forEach2@{ index2, item2 ->
        if (index2 <= index1 || abs(index1 - index2) > (BigMetaService.MAX_ORDER_DISTANCE + 1)) {
          return@forEach2
        }
        val key2Id = keyIdMap[item2.namespace to item2.keyName] ?: return@forEach2

        val distance = distances[min(key1Id, key2Id) to max(key1Id, key2Id)]
          ?: createDistance(key1Id, key2Id)
        relevant[distance.key1Id to distance.key2Id] = distance
        distance.score = computeDistanceScore(distance.score, distance.hits, relatedKeysSize, index1, index2)
        distance.hits++
      }
    }
  }

  private fun decreaseOthers() {
    existing.forEach {
      if (relevant[it.key1Id to it.key2Id] == null) {
        it.score = it.score * it.hits / (it.hits + 1)
        it.hits++
      }
    }
  }

  private val keys by lazy {
    bigMetaService.getKeyIdsForItems(relatedKeysInOrder, project.id)
  }

  private val relevant = mutableMapOf<Pair<Long, Long>, KeysDistance>()

  private val keyIdMap by lazy {
    keys.associate { (it.namespace to it.name) to it.id }
  }

  private val existing by lazy {
    bigMetaService.findExistingKeyDistances(keys, project)
  }

  private val distances by lazy {
    existing.associateBy {
      (it.key1Id to it.key2Id)
    }.toMutableMap()
  }

  private val relatedKeysSize = relatedKeysInOrder.size

  private fun createDistance(key1Id: Long, key2Id: Long): KeysDistance {
    return KeysDistance()
      .apply {
        this.key1Id = Longs.min(key1Id, key2Id)
        this.key2Id = max(key1Id, key2Id)
        this.project = this@KeysDistanceUtil.project
        this.new = true
        distances[this.key1Id to this.key2Id] = this
      }
  }

  private fun computeDistanceScore(
    oldDistance: Long,
    hits: Long,
    relatedKeysSize: Int,
    index1: Int,
    index2: Int
  ): Long {
    val maxDistance = (relatedKeysSize - 2)

    val points = (
      (maxDistance - (abs(index1 - index2) - 1)) / maxDistance.toDouble()
      ) * BigMetaService.MAX_POINTS

    val baseDistance = BigMetaService.MAX_DISTANCE_SCORE - BigMetaService.MAX_POINTS
    return (oldDistance * hits + baseDistance + points).toLong() / (hits + 1)
  }
}
