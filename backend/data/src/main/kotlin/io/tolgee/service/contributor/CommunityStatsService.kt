package io.tolgee.service.contributor

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.community.Bucket
import io.tolgee.dtos.community.CommunityActivity
import io.tolgee.dtos.community.CommunityActivityItem
import io.tolgee.dtos.community.CommunityContributorStats
import io.tolgee.dtos.community.CommunityProfile
import io.tolgee.dtos.community.Granularity
import io.tolgee.dtos.community.LanguageCounts
import io.tolgee.dtos.community.ProfileLanguage
import io.tolgee.dtos.community.ProfileProject
import io.tolgee.dtos.request.community.PeriodParams
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
import java.time.LocalDate

/** The counters are approximate and `active` is an exact live count, so never compute one from the other. */
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
    params: PeriodParams,
    languageIds: Collection<Long>?,
    excludeEmpty: Boolean,
  ): Page<CommunityContributorStats> {
    val period = period(params)
    val languages = languageIds?.takeIf { it.isNotEmpty() }
    val totals =
      repository.findContributorTotals(
        projectId,
        period.from,
        period.to,
        languages,
        excludeEmpty,
        allowedSort(pageable),
      )
    // ponytail: loads every counter row of the page's users; fine while a user has days × languages rows.
    val dayRowsByUser =
      totals.content
        .map { it.id }
        .takeIf { it.isNotEmpty() }
        ?.let { repository.findDayRows(projectId, it, period.from, period.to, languages).groupBy { row -> row.userId } }
        .orEmpty()
    return totals.map { total -> contributorStats(total, dayRowsByUser[total.id].orEmpty(), period) }
  }

  private fun contributorStats(
    total: CommunityContributorTotalsView,
    dayRows: List<CommunityDayRowView>,
    period: Period,
  ) = CommunityContributorStats(
    id = total.id,
    name = total.name,
    avatar = avatarService.getAvatarLinks(total.avatarHash),
    contributions = total.contributions,
    suggested = total.suggested,
    accepted = total.accepted,
    declined = total.declined,
    active = total.active,
    firstContributionAt = total.firstContributionAt,
    lastContributionAt = total.lastContributionAt,
    languages = languageCounts(dayRows),
    buckets = buckets(period, dayRows.map { it.day to it.counts() }),
  )

  fun getActivity(
    projectId: Long,
    params: PeriodParams,
    languageIds: Collection<Long>?,
  ): CommunityActivity {
    val period = period(params)
    val languages = languageIds?.takeIf { it.isNotEmpty() }
    val days = repository.findDailyTotals(projectId, period.from, period.to, languages).map { it.day to it.counts() }
    val tiles = repository.countSuggestedTranslations(projectId, period.from, period.to, languages)
    return CommunityActivity(
      buckets = buckets(period, days),
      translationsWithPendingSuggestion = tiles.pending,
      translationsWithAnySuggestion = tiles.any,
    )
  }

  fun getProfile(
    userId: Long,
    params: PeriodParams,
  ): CommunityProfile? {
    val user = userAccountService.findActive(userId) ?: return null
    if (!isProfileVisible(userId)) return null
    // ponytail: one query, all of the user's public rows, aggregated here; ceiling = days × languages × projects.
    val rows = repository.findPublicRowsOfUser(userId)
    val totals = rows.map { it.counts() }.sum()
    val activeByProject = repository.findPublicActiveSuggestionsOfUser(userId).associate { it.projectId to it.active }
    val period =
      period(params, defaultFrom = { to -> to.minusDays(HEATMAP_DAYS - 1) }, defaultGranularity = Granularity.DAY)
    return CommunityProfile(
      id = user.id,
      name = user.name,
      avatar = avatarService.getAvatarLinks(user.avatarHash),
      contributions = totals.contributions,
      suggested = totals.suggested,
      accepted = totals.accepted,
      declined = totals.declined,
      active = activeByProject.values.sum(),
      languages = profileLanguages(rows),
      projects = profileProjects(rows, activeByProject),
      buckets = buckets(period, rows.map { it.day to it.counts() }),
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

  private fun profileProjects(
    rows: List<CommunityProfileRowView>,
    activeByProject: Map<Long, Long>,
  ): List<ProfileProject> =
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
          activeByProject[id] ?: 0,
          r.first().firstContributionAt,
          r.first().lastContributionAt,
        )
      }.sortedWith(compareByDescending<ProfileProject> { it.contributions }.thenBy { it.id })

  private fun period(
    params: PeriodParams,
    defaultFrom: (LocalDate) -> LocalDate = { to -> Granularity.WEEK.start(to).minusWeeks(DEFAULT_WEEKS - 1) },
    defaultGranularity: Granularity = Granularity.WEEK,
  ): Period {
    val to = params.to ?: currentDateProvider.localDate
    val from = (params.from ?: defaultFrom(to)).coerceIn(to.minusYears(MAX_YEARS), to)
    return Period(from, to, params.granularity ?: defaultGranularity)
  }

  private fun buckets(
    period: Period,
    days: List<Pair<LocalDate, Counts>>,
  ): List<Bucket> {
    val g = period.granularity
    val byBucket =
      days
        .filter { (day) -> day in period.from..period.to }
        .groupBy({ (day) -> g.start(day) }, { (_, c) -> c })
    // ponytail: ~730 buckets per item at DAY granularity over the 2-year cap; cap the page size if it shows.
    return generateSequence(g.start(period.from), g::next)
      .takeWhile { it <= period.to }
      .map { start ->
        val c = byBucket[start].orEmpty().sum()
        Bucket(start.toString(), c.contributions, c.suggested, c.accepted, c.declined)
      }.toList()
  }

  private fun allowedSort(pageable: Pageable): Pageable {
    val orders =
      pageable.sort
        .filter { it.property in SORTABLE }
        .map { it.nullsLast() }
        .toMutableList()
    if (orders.isEmpty()) {
      orders.add(Sort.Order.desc("contributions"))
      orders.add(Sort.Order.desc("suggested"))
    }
    orders.add(Sort.Order.asc("id"))
    return PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(orders))
  }

  private data class Period(
    val from: LocalDate,
    val to: LocalDate,
    val granularity: Granularity,
  )

  private data class Counts(
    val contributions: Long,
    val suggested: Long,
    val accepted: Long,
    val declined: Long,
  ) {
    operator fun plus(o: Counts) =
      Counts(contributions + o.contributions, suggested + o.suggested, accepted + o.accepted, declined + o.declined)
  }

  private fun Iterable<Counts>.sum() = fold(Counts(0, 0, 0, 0)) { a, b -> a + b }

  private fun CommunityDayRowView.counts() =
    Counts(contributions.toLong(), suggested.toLong(), accepted.toLong(), declined.toLong())

  private fun CommunityProfileRowView.counts() =
    Counts(contributions.toLong(), suggested.toLong(), accepted.toLong(), declined.toLong())

  private fun DayCountsView.counts() = Counts(contributions, suggested, accepted, declined)

  companion object {
    private const val DEFAULT_WEEKS = 12L
    private const val MAX_YEARS = 2L
    private const val HEATMAP_DAYS = 365L
    private val SORTABLE = setOf("contributions", "suggested", "accepted", "declined", "active", "lastContributionAt")

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
