package io.tolgee.service.contributor

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.community.CommunityActivity
import io.tolgee.dtos.community.CommunityActivityItem
import io.tolgee.dtos.community.CommunityContributorStats
import io.tolgee.dtos.community.CommunityProfile
import io.tolgee.dtos.community.DayCounts
import io.tolgee.dtos.community.LanguageCounts
import io.tolgee.dtos.community.ProfileLanguage
import io.tolgee.dtos.community.ProfileProject
import io.tolgee.dtos.community.WeekBucket
import io.tolgee.model.views.CommunityContributorTotalsView
import io.tolgee.model.views.CommunityDayRowView
import io.tolgee.model.views.CommunityProfileRowView
import io.tolgee.model.views.DayCountsView
import io.tolgee.repository.CommunityContributionDayRepository
import io.tolgee.service.AvatarService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class CommunityStatsService(
  private val repository: CommunityContributionDayRepository,
  private val activityService: ActivityService,
  private val avatarService: AvatarService,
  private val userAccountService: UserAccountService,
  private val projectService: ProjectService,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun getContributors(
    projectId: Long,
    pageable: Pageable,
    weeks: Int,
  ): Page<CommunityContributorStats> {
    val totals = repository.findContributorTotals(projectId, allowedSort(pageable))
    // ponytail: loads every counter row of the page's users; fine while a user has days × languages rows.
    val dayRowsByUser =
      totals.content
        .map { it.id }
        .takeIf { it.isNotEmpty() }
        ?.let { repository.findDayRows(projectId, it).groupBy { row -> row.userId } }
        .orEmpty()
    val weekStarts = weekStarts(weeks)
    return totals.map { total ->
      contributorStats(total, dayRowsByUser[total.id].orEmpty(), weekStarts)
    }
  }

  fun getContributor(
    projectId: Long,
    userId: Long,
    weeks: Int,
  ): CommunityContributorStats? {
    val total = repository.findContributorTotal(projectId, userId) ?: return null
    return contributorStats(total, repository.findDayRows(projectId, listOf(userId)), weekStarts(weeks))
  }

  private fun contributorStats(
    total: CommunityContributorTotalsView,
    dayRows: List<CommunityDayRowView>,
    weekStarts: List<LocalDate>,
  ) = CommunityContributorStats(
    id = total.id,
    name = total.name,
    avatar = avatarService.getAvatarLinks(total.avatarHash),
    contributions = total.contributions,
    suggested = total.suggested,
    accepted = total.accepted,
    declined = total.declined,
    firstContributionAt = total.firstContributionAt,
    lastContributionAt = total.lastContributionAt,
    languages = languageCounts(dayRows),
    weeks = weeklyBuckets(weekStarts, dayRows.map { it.day to it.counts() }),
  )

  fun getActivity(
    projectId: Long,
    weeks: Int,
  ): CommunityActivity {
    val weekStarts = weekStarts(weeks)
    val days = repository.findDailyTotals(projectId, weekStarts.first()).map { it.day to it.counts() }
    return CommunityActivity(weeklyBuckets(weekStarts, days))
  }

  fun getProfile(userId: Long): CommunityProfile? {
    val user = userAccountService.findActive(userId) ?: return null
    if (!isProfileVisible(userId)) return null
    // ponytail: one query, all of the user's public rows, aggregated here; ceiling = days × languages × projects.
    val rows = repository.findPublicRowsOfUser(userId)
    val totals = rows.map { it.counts() }.sum()
    return CommunityProfile(
      id = user.id,
      name = user.name,
      avatar = avatarService.getAvatarLinks(user.avatarHash),
      contributions = totals.contributions,
      suggested = totals.suggested,
      accepted = totals.accepted,
      declined = totals.declined,
      languages = profileLanguages(rows),
      projects = profileProjects(rows),
      days = profileDays(rows),
    )
  }

  fun getActivityFeed(
    userId: Long,
    pageable: Pageable,
  ): Page<CommunityActivityItem>? {
    if (userAccountService.findActive(userId) == null || !isProfileVisible(userId)) return null
    val newestFirst = PageRequest.of(pageable.pageNumber, pageable.pageSize)
    val page = activityService.findPublicContributionsOfUser(userId, CONTRIBUTION_TYPES, newestFirst)
    val projects = projectService.findAll(page.content.mapNotNull { it.projectId }.toSet()).associateBy { it.id }
    return page.map { CommunityActivityItem(projects.getValue(it.projectId!!), it) }
  }

  private fun isProfileVisible(userId: Long): Boolean =
    repository.findPublicProjectIds(userId, PageRequest.of(0, 1)).isNotEmpty()

  private fun languageCounts(dayRows: List<CommunityDayRowView>): List<LanguageCounts> =
    dayRows
      .groupBy { it.languageId }
      .map { (languageId, rows) ->
        val c = rows.map { it.counts() }.sum()
        LanguageCounts(languageId, c.contributions, c.suggested, c.accepted, c.declined)
      }.sortedWith(compareByDescending<LanguageCounts> { it.contributions }.thenBy { it.languageId })

  private fun profileLanguages(rows: List<CommunityProfileRowView>): List<ProfileLanguage> =
    rows
      .groupBy { it.languageTag }
      .map { (tag, r) ->
        val c = r.map { it.counts() }.sum()
        ProfileLanguage(tag, r.first().languageName, c.contributions, c.suggested, c.accepted, c.declined)
      }.sortedWith(compareByDescending<ProfileLanguage> { it.contributions }.thenBy { it.tag })

  private fun profileProjects(rows: List<CommunityProfileRowView>): List<ProfileProject> =
    rows
      .groupBy { it.projectId }
      .map { (id, r) ->
        val c = r.map { it.counts() }.sum()
        ProfileProject(
          id,
          r.first().projectName,
          avatarService.getAvatarLinks(r.first().projectAvatarHash),
          c.contributions,
          c.suggested,
          c.accepted,
          c.declined,
          r.first().firstContributionAt,
          r.first().lastContributionAt,
        )
      }.sortedWith(compareByDescending<ProfileProject> { it.contributions }.thenBy { it.id })

  private fun profileDays(rows: List<CommunityProfileRowView>): List<DayCounts> {
    val since = currentDateProvider.localDate.minusDays(HEATMAP_DAYS)
    return rows
      .filter { it.day > since }
      .groupBy { it.day }
      .map { (day, r) -> r.map { it.counts() }.sum().onDay(day) }
      .sortedBy { it.day }
  }

  private fun weekStarts(weeks: Int): List<LocalDate> {
    val currentWeek = currentDateProvider.localDate.weekStart()
    return (weeks.coerceIn(1, MAX_WEEKS) - 1 downTo 0).map { currentWeek.minusWeeks(it.toLong()) }
  }

  private fun weeklyBuckets(
    weekStarts: List<LocalDate>,
    days: List<Pair<LocalDate, Counts>>,
  ): List<WeekBucket> {
    val byWeek =
      days.groupBy({ (day) -> day.weekStart() }, { (_, c) -> c })
    return weekStarts.map { start ->
      val c = byWeek[start].orEmpty().sum()
      WeekBucket(start.toString(), c.contributions, c.suggested, c.accepted, c.declined)
    }
  }

  private fun allowedSort(pageable: Pageable): Pageable {
    val orders = pageable.sort.filter { it.property in SORTABLE }.toMutableList()
    if (orders.isEmpty()) {
      orders.add(Sort.Order.desc("contributions"))
      orders.add(Sort.Order.desc("suggested"))
    }
    orders.add(Sort.Order.asc("id"))
    return PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(orders))
  }

  private fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

  private data class Counts(
    val contributions: Long,
    val suggested: Long,
    val accepted: Long,
    val declined: Long,
  ) {
    operator fun plus(o: Counts) =
      Counts(contributions + o.contributions, suggested + o.suggested, accepted + o.accepted, declined + o.declined)

    fun onDay(day: LocalDate) = DayCounts(day.toString(), contributions, suggested, accepted, declined)
  }

  private fun Iterable<Counts>.sum() = fold(Counts(0, 0, 0, 0)) { a, b -> a + b }

  private fun CommunityDayRowView.counts() =
    Counts(contributions.toLong(), suggested.toLong(), accepted.toLong(), declined.toLong())

  private fun CommunityProfileRowView.counts() =
    Counts(contributions.toLong(), suggested.toLong(), accepted.toLong(), declined.toLong())

  private fun DayCountsView.counts() = Counts(contributions, suggested, accepted, declined)

  companion object {
    const val DEFAULT_WEEKS = 12
    const val MAX_WEEKS = 104
    private const val HEATMAP_DAYS = 365L
    private val SORTABLE = setOf("contributions", "suggested", "accepted", "declined")

    /** Mirrors `is_translation_contribution` (`db/changelog/isTranslationContribution.sql`). */
    val CONTRIBUTION_TYPES =
      listOf(
        ActivityType.SET_TRANSLATIONS,
        ActivityType.SET_TRANSLATION_STATE,
        ActivityType.COMPLEX_EDIT,
        ActivityType.TRANSLATION_COMMENT_ADD,
        ActivityType.TRANSLATION_COMMENT_EDIT,
        ActivityType.TRANSLATION_COMMENT_DELETE,
        ActivityType.TRANSLATION_COMMENT_SET_STATE,
        ActivityType.CREATE_SUGGESTION,
        ActivityType.ACCEPT_SUGGESTION,
        ActivityType.DECLINE_SUGGESTION,
        ActivityType.REVERSE_SUGGESTION,
        ActivityType.DELETE_SUGGESTION,
        ActivityType.SUGGESTION_SET_ACTIVE,
      )
  }
}
