package io.tolgee.service.bigMeta

import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.model.Project
import io.tolgee.util.Logging
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MAX_STORED = 20

class KeysDistanceUtil(
  private val relatedKeysInOrder: MutableList<RelatedKeyDto>,
  private val project: Project,
  private val bigMetaService: BigMetaService,
) : Logging {
  val toStoreAndDelete by lazy {
    val toStore = mutableSetOf<KeysDistanceDto>()
    val toDelete = mutableSetOf<KeysDistanceDto>()
    val allTouchedIds = (keys.map { it.id }).toSet()
    val updatedByIdMap =
      allTouchedIds.associateWith { keyId ->
        allUpdated.filterKeys { (key1Id, key2Id) -> key1Id == keyId || key2Id == keyId }.values
      }
    updatedByIdMap.values.forEach { distancesOdKey ->
      val sortedByDistance = distancesOdKey.sortedBy { it.distance }
      toStore.addAll(sortedByDistance.take(MAX_STORED))
      toDelete.addAll(sortedByDistance.drop(MAX_STORED).filter { it.stored })
    }

    // for some keys, distance might be out of the window, but for others,
    // it can still be in the window, we don't want to delete such distances
    toDelete.removeAll(toStore)

    toStore to toDelete
  }

  private val allUpdated by lazy { currentUpdated + otherThanCurrentUpdated }

  /**
   * Updated values for currently provided distances
   */
  private val currentUpdated by lazy {
    val result: DistancesMutableMap = mutableMapOf()
    distinctKeys.forEachIndexed forEach1@{ index1, item1 ->
      val key1Id = getKeyId(item1.namespace, item1.keyName) ?: return@forEach1
      distinctKeys.forEachIndexed forEach2@{ index2, item2 ->
        if (index2 >= index1) return@forEach2
        val key2Id = getKeyId(item2.namespace, item2.keyName) ?: return@forEach2
        val distance =
          distances
            .get(key1Id, key2Id)
            ?.also {
              it.distance = computeDistance(it.distance, it.hits, index1 = index1, index2 = index2)
              it.hits++
            }
            ?: let {
              val newDistance = computeDistance(index1 = index1, index2 = index2)
              createDistance(key1Id, key2Id, newDistance)
            }
        result.add(key1Id, key2Id, distance)
      }
    }
    result
  }

  /**
   * We pretend that keys not included in the current list are just after the current,
   * so we make them distances higher than the max distance
   */
  private val otherThanCurrentUpdated by lazy {
    // Distances that are not currently provided
    val otherThanCurrent =
      existing.filter {
        !currentUpdated.containsKey(it.key)
      }

    distinctKeys.map { getKeyId(it.namespace, it.keyName) }.forEachIndexed { index, keyId ->
      // by this, we are pushing unprovided keys out of the "focus zone", so they should "converge" to become deleted
      val maxDistance = MAX_STORED
      otherThanCurrent
        .asSequence()
        .filter {
          it.key.first == keyId || it.key.second == keyId
        }.sortedBy { it.value.distance }
        .forEachIndexed { index, (key, value) ->
          value.distance =
            computeDistance(
              oldDistance = value.distance,
              hits = value.hits,
              newDistance = maxDistance,
            )
          value.hits++
        }
    }

    otherThanCurrent
  }

  private val keys by lazy {
    bigMetaService.getKeyIdsForItems(relatedKeysInOrder, project.id)
  }

  private val distinctKeys by lazy {
    relatedKeysInOrder.distinct()
  }

  private val keyIdMap by lazy {
    keys.associate { (it.namespace to it.name) to it.id }
  }

  private val existing: DistancesMap by lazy {
    bigMetaService
      .findExistingKeyDistances(keys, project)
      .associateBy {
        (it.key1Id to it.key2Id)
      }
  }

  private fun getKeyId(
    namespace: String?,
    keyName: String,
  ): Long? {
    return keyIdMap[namespace to keyName]
  }

  private val distances: DistancesMutableMap by lazy {
    existing.toMutableMap()
  }

  private fun DistancesMutableMap.add(
    key1Id: Long,
    key2Id: Long,
    distance: KeysDistanceDto,
  ) {
    this[getDistancesMapKey(key1Id, key2Id)] = distance
  }

  private fun DistancesMap.containsKey(
    key1Id: Long,
    key2Id: Long,
  ): Boolean {
    return this.containsKey(getDistancesMapKey(key1Id, key2Id))
  }

  fun DistancesMap.get(
    key1Id: Long,
    key2Id: Long,
  ): KeysDistanceDto? {
    return this[getDistancesMapKey(key1Id, key2Id)]
  }

  private fun getDistancesMapKey(
    key1Id: Long,
    key2Id: Long,
  ) = min(key1Id, key2Id) to max(key1Id, key2Id)

  private fun createDistance(
    key1Id: Long,
    key2Id: Long,
    newDistance: Double,
  ): KeysDistanceDto {
    return KeysDistanceDto(
      key1Id = min(a = key1Id, b = key2Id),
      key2Id = max(key1Id, key2Id),
      projectId = project.id,
      hits = 1,
      distance = newDistance,
      stored = false,
    ).apply {
      distances[this.key1Id to this.key2Id] = this
    }
  }

  private fun computeDistance(
    oldDistance: Double = 0.0,
    hits: Long = 0,
    index1: Int,
    index2: Int,
  ): Double {
    val newDistance = abs(index1 - index2) - 1
    return computeDistance(oldDistance, hits, newDistance)
  }

  private fun computeDistance(
    oldDistance: Double = 0.0,
    hits: Long = 0,
    newDistance: Int,
  ): Double {
    return (oldDistance * hits + newDistance) / (hits + 1)
  }
}

private typealias DistancesMutableMap = MutableMap<Pair<Long, Long>, KeysDistanceDto>
private typealias DistancesMap = Map<Pair<Long, Long>, KeysDistanceDto>
