package io.tolgee.service.contributor

import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.community.CommunityActivity
import io.tolgee.dtos.community.CommunityContributorStats
import io.tolgee.dtos.community.CommunityProfile
import io.tolgee.dtos.community.DayCounts
import io.tolgee.dtos.community.LanguageCounts
import io.tolgee.dtos.community.ProfileLanguage
import io.tolgee.dtos.community.ProfileProject
import io.tolgee.dtos.community.RecentContribution
import io.tolgee.dtos.community.WeekBucket
import io.tolgee.model.views.CommunityDayRowView
import io.tolgee.model.views.CommunityProfileRowView
import io.tolgee.model.views.DayCountsView
import io.tolgee.repository.CommunityContributionDayRepository
import io.tolgee.service.AvatarService
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
  private val avatarService: AvatarService,
  private val userAccountService: UserAccountService,
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
      val dayRows = dayRowsByUser[total.id].orEmpty()
      CommunityContributorStats(
        id = total.id,
        name = total.name,
        avatar = avatarService.getAvatarLinks(total.avatarHash),
        suggested = total.suggested,
        accepted = total.accepted,
        declined = total.declined,
        languages = languageCounts(dayRows),
        weeks = weeklyBuckets(weekStarts, dayRows.map { it.day to it.counts() }),
      )
    }
  }

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
    // ponytail: one query, all of the user's public rows, aggregated here; ceiling = days × languages × projects.
    val rows = repository.findPublicRowsOfUser(userId)
    if (rows.isEmpty()) return null
    val totals = rows.map { it.counts() }.sum()
    return CommunityProfile(
      id = user.id,
      name = user.name,
      avatar = avatarService.getAvatarLinks(user.avatarHash),
      suggested = totals.suggested,
      accepted = totals.accepted,
      declined = totals.declined,
      languages = profileLanguages(rows),
      projects = profileProjects(rows),
      days = profileDays(rows),
      recent = recentContributions(rows),
    )
  }

  private fun languageCounts(dayRows: List<CommunityDayRowView>): List<LanguageCounts> =
    dayRows
      .groupBy { it.languageId }
      .map { (languageId, rows) ->
        val c = rows.map { it.counts() }.sum()
        LanguageCounts(languageId, c.suggested, c.accepted, c.declined)
      }.sortedWith(compareByDescending<LanguageCounts> { it.suggested }.thenBy { it.languageId })

  private fun profileLanguages(rows: List<CommunityProfileRowView>): List<ProfileLanguage> =
    rows
      .groupBy { it.languageTag }
      .map { (tag, r) ->
        val c = r.map { it.counts() }.sum()
        ProfileLanguage(tag, r.first().languageName, c.suggested, c.accepted, c.declined)
      }.sortedWith(compareByDescending<ProfileLanguage> { it.suggested }.thenBy { it.tag })

  private fun profileProjects(rows: List<CommunityProfileRowView>): List<ProfileProject> =
    rows
      .groupBy { it.projectId }
      .map { (id, r) ->
        val c = r.map { it.counts() }.sum()
        ProfileProject(
          id,
          r.first().projectName,
          avatarService.getAvatarLinks(r.first().projectAvatarHash),
          c.suggested,
          c.accepted,
          c.declined,
        )
      }.sortedWith(compareByDescending<ProfileProject> { it.suggested }.thenBy { it.id })

  private fun profileDays(rows: List<CommunityProfileRowView>): List<DayCounts> {
    val since = currentDateProvider.localDate.minusDays(HEATMAP_DAYS)
    return rows
      .filter { it.day > since }
      .groupBy { it.day }
      .map { (day, r) -> r.map { it.counts() }.sum().onDay(day) }
      .sortedBy { it.day }
  }

  private fun recentContributions(rows: List<CommunityProfileRowView>): List<RecentContribution> =
    rows
      .sortedWith(
        compareByDescending<CommunityProfileRowView> { it.day }.thenBy { it.projectId }.thenBy { it.languageTag },
      ).take(RECENT_LIMIT)
      .map {
        val c = it.counts()
        RecentContribution(
          it.day.toString(),
          it.projectId,
          it.projectName,
          it.languageTag,
          c.suggested,
          c.accepted,
          c.declined,
        )
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
      WeekBucket(start.toString(), c.suggested, c.accepted, c.declined)
    }
  }

  private fun allowedSort(pageable: Pageable): Pageable {
    val orders = pageable.sort.filter { it.property in SORTABLE }.toMutableList()
    if (orders.isEmpty()) {
      orders.add(Sort.Order.desc("accepted"))
      orders.add(Sort.Order.desc("suggested"))
    }
    orders.add(Sort.Order.asc("id"))
    return PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(orders))
  }

  private fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

  private data class Counts(
    val suggested: Long,
    val accepted: Long,
    val declined: Long,
  ) {
    operator fun plus(o: Counts) = Counts(suggested + o.suggested, accepted + o.accepted, declined + o.declined)

    fun onDay(day: LocalDate) = DayCounts(day.toString(), suggested, accepted, declined)
  }

  private fun Iterable<Counts>.sum() = fold(Counts(0, 0, 0)) { a, b -> a + b }

  private fun CommunityDayRowView.counts() = Counts(suggested.toLong(), accepted.toLong(), declined.toLong())

  private fun CommunityProfileRowView.counts() = Counts(suggested.toLong(), accepted.toLong(), declined.toLong())

  private fun DayCountsView.counts() = Counts(suggested, accepted, declined)

  companion object {
    const val DEFAULT_WEEKS = 12
    const val MAX_WEEKS = 104
    private const val HEATMAP_DAYS = 365L
    private const val RECENT_LIMIT = 20
    private val SORTABLE = setOf("suggested", "accepted", "declined")
  }
}
