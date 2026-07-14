package io.tolgee.dtos.request

import io.tolgee.activity.groups.ActivityGroupType

class ActivityGroupFilters {
  var filterType: ActivityGroupType? = null
  var filterLanguageIdIn: List<Long>? = null
  var filterAuthorUserIdIn: List<Long>? = null
}
