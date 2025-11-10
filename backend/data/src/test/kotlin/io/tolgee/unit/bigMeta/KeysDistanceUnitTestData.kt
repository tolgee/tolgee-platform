package io.tolgee.unit.bigMeta

import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.dtos.queryResults.KeyIdFindResult
import io.tolgee.service.bigMeta.KeysDistanceDto
import kotlin.math.abs

class KeysDistanceUnitTestData {
  val requestData = mutableListOf<RelatedKeyDto>()

  private val _existingDistances = mutableSetOf<KeysDistanceDto>()

  val existingDistances: Set<KeysDistanceDto>
    get() {
      val existingKeyIds = existingKeys.map { it.id }.toSet()
      return _existingDistances
        .filter {
          it.key1Id in existingKeyIds || it.key2Id in existingKeyIds
        }.toSet()
    }

  fun createRequestData(
    count: Int,
    namespace: String?,
  ) {
    val created =
      (0 until count).map {
        RelatedKeyDto(keyName = getDefaultKeyName(it), namespace = namespace)
      }
    requestData.addAll(created)
  }

  private fun getDefaultKeyName(it: Number) = "key$it"

  val existingKeys: List<KeyIdFindResult>
    get() {
      return requestData.mapIndexed { index, relatedKeyDto ->
        KeyIdFindResult(
          id = index.toLong(),
          name = relatedKeyDto.keyName,
          namespace = relatedKeyDto.namespace,
        )
      }
    }

  private fun addExistingDistance(
    key1Id: Long,
    key2Id: Long,
    modifyDistance: KeysDistanceDto.() -> Unit = {},
  ) {
    val defaultDistance = abs(key1Id - key2Id) - 1
    val distanceDto =
      KeysDistanceDto(
        key1Id = key1Id,
        key2Id = key2Id,
        projectId = 0,
        hits = 1,
        distance = defaultDistance.toDouble(),
      )
    distanceDto.modifyDistance()
    _existingDistances.add(distanceDto)
  }

  fun generateExistingDistances(range: IntRange) {
    for (i in range) {
      for (j in range) {
        if (i >= j) {
          continue
        }
        addExistingDistance(i.toLong(), j.toLong())
      }
    }
  }
}
