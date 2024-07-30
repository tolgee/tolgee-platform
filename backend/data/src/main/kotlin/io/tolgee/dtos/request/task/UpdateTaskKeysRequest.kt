package io.tolgee.dtos.request.task

data class UpdateTaskKeysRequest(
  var addKeys: MutableSet<Long>? = null,
  var removeKeys: MutableSet<Long>? = null,
)
