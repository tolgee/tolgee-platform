package io.tolgee.service.bigMeta

data class KeysDistanceDto(
  var key1Id: Long = 0,
  var key2Id: Long = 0,
  var distance: Double = 0.0,
  var projectId: Long,
  var hits: Long = 1,
  var stored: Boolean = true,
)
