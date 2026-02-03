package io.tolgee.dtos.queryResults

data class KeyView(
  val id: Long,
  val name: String,
  val namespace: String?,
  val description: String?,
  val custom: Any?,
)
