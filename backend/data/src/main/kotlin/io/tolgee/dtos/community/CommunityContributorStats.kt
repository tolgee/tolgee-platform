package io.tolgee.dtos.community

import io.tolgee.dtos.Avatar

data class CommunityContributorStats(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val languages: List<LanguageCounts>,
  val weeks: List<WeekBucket>,
)
