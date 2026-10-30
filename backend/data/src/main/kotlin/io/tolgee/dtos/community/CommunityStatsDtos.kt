package io.tolgee.dtos.community

import com.fasterxml.jackson.annotation.JsonFormat
import io.tolgee.dtos.Avatar
import io.tolgee.model.Project
import io.tolgee.model.views.activity.ProjectActivityView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Date

enum class Granularity {
  DAY {
    override fun start(day: LocalDate) = day

    override fun next(start: LocalDate) = start.plusDays(1)
  },
  WEEK {
    override fun start(day: LocalDate) = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    override fun next(start: LocalDate) = start.plusWeeks(1)
  },
  ;

  abstract fun start(day: LocalDate): LocalDate

  abstract fun next(start: LocalDate): LocalDate
}

data class Bucket(
  /** ISO date. */
  val start: String,
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
  val active: Long,
  val firstContributionAt: Date?,
  val lastContributionAt: Date?,
  val languages: List<LanguageCounts>,
  val buckets: List<Bucket>,
)

data class CommunityActivity(
  val buckets: List<Bucket>,
  val translationsWithPendingSuggestion: Long,
  val translationsWithAnySuggestion: Long,
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
  val active: Long,
  @get:JsonFormat(shape = JsonFormat.Shape.STRING)
  val firstContributionAt: Date?,
  @get:JsonFormat(shape = JsonFormat.Shape.STRING)
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
  val active: Long,
  val languages: List<ProfileLanguage>,
  val projects: List<ProfileProject>,
  val buckets: List<Bucket>,
)

data class CommunityActivityItem(
  val project: Project,
  val activity: ProjectActivityView,
)
