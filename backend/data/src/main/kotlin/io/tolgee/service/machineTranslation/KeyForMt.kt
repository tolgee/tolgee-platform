package io.tolgee.service.machineTranslation

data class KeyForMt(
  val id: Long,
  val name: String,
  val namespace: String?,
  val description: String?,
  val baseTranslation: String?,
  val isPlural: Boolean,
)
