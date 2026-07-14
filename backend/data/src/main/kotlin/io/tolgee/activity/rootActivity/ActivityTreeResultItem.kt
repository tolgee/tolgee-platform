package io.tolgee.activity.rootActivity

data class ActivityTreeResultItem(
  val entityClass: String,
  val description: Map<String, Any?>?,
  val modifications: Map<String, Any?>?,
  val entityId: Long,
  val type: ActivityItemsParser.Type,
  val parentId: Long?,
  var children: MutableMap<String, Any?>? = null,
)
