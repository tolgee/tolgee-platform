package io.tolgee.dtos.community

import io.tolgee.dtos.Avatar

data class WeekBucket(
  /** ISO date of the week's Monday. */
  val weekStart: String,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class DayCounts(
  /** ISO date. */
  val day: String,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class LanguageCounts(
  val languageId: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

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

data class CommunityActivity(
  val weeks: List<WeekBucket>,
)

data class ProfileLanguage(
  val tag: String,
  val name: String,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class ProfileProject(
  val id: Long,
  val name: String,
  val avatar: Avatar?,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class RecentContribution(
  /** ISO date. */
  val day: String,
  val projectId: Long,
  val projectName: String,
  val languageTag: String,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class CommunityProfile(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val languages: List<ProfileLanguage>,
  val projects: List<ProfileProject>,
  val days: List<DayCounts>,
  val recent: List<RecentContribution>,
)
