package io.tolgee.websocket

import io.tolgee.activity.data.PropertyModification

data class ModifiedKey(
  val id: Long,
  val name: String?,
  val modifications: Map<String, PropertyModification?>
)
