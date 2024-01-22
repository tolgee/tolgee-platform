package io.tolgee.service.bigMeta

import io.tolgee.model.keyBigMeta.KeysDistance.Companion.MAX_SCORE

data class KeysDistanceDto(
  var key1Id: Long = 0,
  var key2Id: Long = 0,
  var score: Long = MAX_SCORE,
  var projectId: Long,
  var hits: Long = 1,
)
