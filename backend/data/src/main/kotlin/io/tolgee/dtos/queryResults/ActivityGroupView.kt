package io.tolgee.dtos.queryResults

import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.api.SimpleUserAccount

data class ActivityGroupView(
  val id: Long,
  val type: ActivityGroupType,
  val timestamp: java.util.Date,
  var data: Any? = null,
  var author: SimpleUserAccount,
  var mentionedLanguageIds: List<Long>,
)
