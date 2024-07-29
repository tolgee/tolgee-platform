package io.tolgee.dtos.queryResults

import io.tolgee.activity.groups.ActivityGroupType

data class ActivityGroupView(
  val id: Long,
  val type: ActivityGroupType,
  val timestamp: java.util.Date,
  /**
   * Counts of items in this group by entity class name
   */
  var counts: Map<String, Int>? = null,
)
