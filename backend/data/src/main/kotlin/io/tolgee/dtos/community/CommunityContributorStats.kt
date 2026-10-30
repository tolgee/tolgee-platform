package io.tolgee.dtos.community

import io.tolgee.dtos.Avatar
import java.util.Date

data class CommunityContributorStats(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val active: Long,
  val firstContributionAt: Date?,
  val lastContributionAt: Date?,
  val languages: List<LanguageCounts>,
)
