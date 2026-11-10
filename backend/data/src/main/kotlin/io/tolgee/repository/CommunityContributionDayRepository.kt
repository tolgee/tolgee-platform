package io.tolgee.repository

import io.tolgee.model.contributor.CommunityContributionDay
import io.tolgee.model.contributor.CommunityContributionDayId
import io.tolgee.model.views.DayCountsView
import io.tolgee.model.views.SuggestedTranslationsView
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import java.time.LocalDate

interface CommunityContributionDayRepository : Repository<CommunityContributionDay, CommunityContributionDayId> {
  @Query(
    """
      select c.day as day, sum(c.contributions) as contributions,
        sum(c.suggested) as suggested, sum(c.accepted) as accepted, sum(c.declined) as declined
      from CommunityContributionDay c
      join UserAccount u on u.id = c.userId
      join Language l on l.id = c.languageId
      where c.projectId = :projectId and $IN_RANGE and $LANGUAGE_FILTER and $LIVE_USER_AND_LANGUAGE
      group by c.day
    """,
  )
  fun findDailyTotals(
    projectId: Long,
    from: LocalDate,
    to: LocalDate,
    languageIds: Collection<Long>?,
  ): List<DayCountsView>

  @Query(
    """
      select count(distinct $TRANSLATION_KEY) as any,
        count(distinct case when s.state = $ACTIVE then $TRANSLATION_KEY end) as pending
      from TranslationSuggestion s
      where s.project.id = :projectId and cast(s.createdAt as date) between :from and :to
        and s.language.deletedAt is null and $LANGUAGE_FILTER_S
    """,
  )
  fun countSuggestedTranslations(
    projectId: Long,
    from: LocalDate,
    to: LocalDate,
    languageIds: Collection<Long>?,
  ): SuggestedTranslationsView

  companion object {
    const val LIVE_USER_AND_LANGUAGE = "u.deletedAt is null and u.disabledAt is null and l.deletedAt is null"
    const val IN_RANGE = "c.day between :from and :to"
    const val LANGUAGE_FILTER = "(:languageIds is null or c.languageId in :languageIds)"
    const val LANGUAGE_FILTER_S = "(:languageIds is null or s.language.id in :languageIds)"
    const val ACTIVE = "io.tolgee.model.enums.TranslationSuggestionState.ACTIVE"
    const val TRANSLATION_KEY = "concat(cast(s.key.id as string), ':', cast(s.language.id as string))"
  }
}
