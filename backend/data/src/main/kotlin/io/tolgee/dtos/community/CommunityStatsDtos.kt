package io.tolgee.dtos.community

import io.tolgee.dtos.Avatar
import io.tolgee.model.Project
import io.tolgee.model.views.activity.ProjectActivityView
import java.util.Date

data class WeekBucket(
  /** ISO date of the week's Monday. */
  val weekStart: String,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class DayCounts(
  /** ISO date. */
  val day: String,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class LanguageCounts(
  val languageId: Long,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class CommunityContributorStats(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val firstContributionAt: Date?,
  val lastContributionAt: Date?,
  val languages: List<LanguageCounts>,
  val weeks: List<WeekBucket>,
)

data class CommunityActivity(
  val weeks: List<WeekBucket>,
)

data class ProfileLanguage(
  val tag: String,
  val name: String,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)

data class ProfileProject(
  val id: Long,
  val name: String,
  val avatar: Avatar?,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val firstContributionAt: Date?,
  val lastContributionAt: Date?,
)

data class CommunityProfile(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val languages: List<ProfileLanguage>,
  val projects: List<ProfileProject>,
  val days: List<DayCounts>,
)

data class CommunityActivityItem(
  val project: Project,
  val activity: ProjectActivityView,
)
