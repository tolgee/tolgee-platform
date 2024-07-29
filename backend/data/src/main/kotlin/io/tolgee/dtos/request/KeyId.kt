package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

class KeyId(
  val name: String?,
  val namespace: String?,
  @Schema(description = "If key id is provided, name and namespace are ignored.")
  val id: Long?,
)
