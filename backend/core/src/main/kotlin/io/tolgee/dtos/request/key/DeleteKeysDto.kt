package io.tolgee.dtos.request.key

import io.swagger.v3.oas.annotations.media.Schema

data class DeleteKeysDto(
  @Schema(description = "IDs of keys to delete")
  var ids: Set<Long> = setOf(),
)
