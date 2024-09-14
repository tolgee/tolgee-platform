package io.tolgee.activity.groups

import java.util.*

data class ActivityGroupDto(
  val id: Long,
  val activityGroupType: ActivityGroupType,
  val latestTimestamp: Date,
  val earliestTimestamp: Date,
  val matchingString: String? = null,
)
