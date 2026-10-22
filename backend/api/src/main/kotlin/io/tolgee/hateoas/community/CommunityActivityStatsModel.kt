package io.tolgee.hateoas.community

import io.tolgee.dtos.community.WeekBucket

@Suppress("unused")
open class CommunityActivityStatsModel(
  val weeks: List<WeekBucket>,
)
