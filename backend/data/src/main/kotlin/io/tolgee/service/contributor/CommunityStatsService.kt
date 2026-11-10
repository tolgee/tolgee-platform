package io.tolgee.service.contributor

import io.tolgee.activity.data.ActivityType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.community.Bucket
import io.tolgee.dtos.community.CommunityActivity
import io.tolgee.dtos.community.Granularity
import io.tolgee.dtos.request.community.PeriodParams
import io.tolgee.dtos.request.community.RangeParams
import io.tolgee.model.views.DayCountsView
import io.tolgee.repository.CommunityContributionDayRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CommunityStatsService(
  private val repository: CommunityContributionDayRepository,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun getActivity(
    projectId: Long,
    params: PeriodParams,
    languageIds: Collection<Long>?,
  ): CommunityActivity {
    val period = period(params)
    val languages = languageFilter(languageIds)
    val days = repository.findDailyTotals(projectId, period.from, period.to, languages).map { it.day to it.counts() }
    val suggestedTranslations = repository.countSuggestedTranslations(projectId, period.from, period.to, languages)
    return CommunityActivity(
      buckets = buckets(period, days),
      translationsWithPendingSuggestion = suggestedTranslations.pending,
      translationsWithAnySuggestion = suggestedTranslations.any,
    )
  }

  private fun languageFilter(languageIds: Collection<Long>?) = languageIds?.takeIf { it.isNotEmpty() }

  private fun period(params: PeriodParams): Period {
    val (from, to) = range(params)
    return Period(from, to, params.granularity ?: Granularity.WEEK)
  }

  private fun range(params: RangeParams): Pair<LocalDate, LocalDate> {
    val today = currentDateProvider.localDate
    val to = (params.to ?: today).coerceIn(LocalDate.EPOCH, today.plusYears(1))
    val from = params.from?.coerceIn(to.minusYears(MAX_YEARS), to) ?: DEFAULT_FROM(to)
    return Pair(from, to)
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
    return generateSequence(g.start(period.from), g::next)
      .takeWhile { it <= period.to }
      .map { start ->
        val c = byBucket[start].orEmpty().sum()
        Bucket(start.toString(), c.contributions, c.suggested, c.accepted, c.declined)
      }.toList()
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

  private fun DayCountsView.counts() = Counts(contributions, suggested, accepted, declined)

  companion object {
    private const val DEFAULT_WEEKS = 12L
    private const val MAX_YEARS = 2L
    private val DEFAULT_FROM: (LocalDate) -> LocalDate =
      { to -> Granularity.WEEK.start(to).minusWeeks(DEFAULT_WEEKS - 1) }

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
