package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateTaskKeysRequest(
  @Schema(
    description = "Keys to add to task",
  )
  var addKeys: MutableSet<Long>? = null,
  @Schema(
    description = "Keys to remove from task",
  )
  var removeKeys: MutableSet<Long>? = null,
)
