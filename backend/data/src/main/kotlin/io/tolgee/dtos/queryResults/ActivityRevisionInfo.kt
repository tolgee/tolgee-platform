package io.tolgee.dtos.queryResults

import io.tolgee.activity.data.ActivityType

data class ActivityRevisionInfo(
  val id: Long,
  val projectId: Long?,
  val modifiedEntityCount: Int,
  val type: ActivityType,
  val isTranslationModification: Boolean,
)
