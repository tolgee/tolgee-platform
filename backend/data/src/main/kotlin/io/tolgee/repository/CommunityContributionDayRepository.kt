package io.tolgee.repository

import io.tolgee.model.contributor.CommunityContributionDay
import io.tolgee.model.contributor.CommunityContributionDayId
import io.tolgee.model.views.CommunityContributorTotalsView
import io.tolgee.model.views.CommunityDayRowView
import io.tolgee.model.views.CommunityProfileRowView
import io.tolgee.model.views.DayCountsView
import io.tolgee.model.views.ProjectActiveSuggestionsView
import io.tolgee.model.views.SuggestedTranslationsView
import io.tolgee.repository.ProjectRepository.Companion.PUBLIC_PROJECT_VISIBILITY
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import java.time.LocalDate

interface CommunityContributionDayRepository : Repository<CommunityContributionDay, CommunityContributionDayId> {
  @Query(
    """
      select c.userId as id, u.name as name, u.avatarHash as avatarHash,
        sum(c.contributions) as contributions,
        sum(c.suggested) as suggested, sum(c.accepted) as accepted, sum(c.declined) as declined,
        pc.firstContributionAt as firstContributionAt, pc.lastContributionAt as lastContributionAt,
        (select count(s) from TranslationSuggestion s
          where s.author.id = c.userId and s.project.id = :projectId and $LIVE_ACTIVE_S and $LANGUAGE_FILTER_S) as active
      from CommunityContributionDay c
      join UserAccount u on u.id = c.userId
      join Language l on l.id = c.languageId
      left join ProjectContributor pc on pc.projectId = c.projectId and pc.userId = c.userId
      where c.projectId = :projectId and $IN_RANGE and $LANGUAGE_FILTER and $LIVE_USER_AND_LANGUAGE
      group by c.userId, u.name, u.avatarHash, pc.firstContributionAt, pc.lastContributionAt
      having (:excludeEmpty = false or sum(c.suggested) > 0)
    """,
    countQuery = """
      select count(distinct c.userId)
      from CommunityContributionDay c
      join UserAccount u on u.id = c.userId
      join Language l on l.id = c.languageId
      where c.projectId = :projectId and $IN_RANGE and $LANGUAGE_FILTER and $LIVE_USER_AND_LANGUAGE
        and (:excludeEmpty = false or c.suggested > 0)
    """,
  )
  fun findContributorTotals(
    projectId: Long,
    from: LocalDate,
    to: LocalDate,
    languageIds: Collection<Long>?,
    excludeEmpty: Boolean,
    pageable: Pageable,
  ): Page<CommunityContributorTotalsView>

  @Query(
    """
      select c.userId as userId, c.languageId as languageId, c.day as day, c.contributions as contributions,
        c.suggested as suggested, c.accepted as accepted, c.declined as declined
      from CommunityContributionDay c
      join Language l on l.id = c.languageId
      where c.projectId = :projectId and c.userId in :userIds and $IN_RANGE and $LANGUAGE_FILTER
        and l.deletedAt is null
    """,
  )
  fun findDayRows(
    projectId: Long,
    userIds: Collection<Long>,
    from: LocalDate,
    to: LocalDate,
    languageIds: Collection<Long>?,
  ): List<CommunityDayRowView>

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

  @Query(
    """
      select c.day as day, c.contributions as contributions,
        c.suggested as suggested, c.accepted as accepted, c.declined as declined,
        l.tag as languageTag, l.name as languageName,
        r.id as projectId, r.name as projectName, r.avatarHash as projectAvatarHash,
        pc.firstContributionAt as firstContributionAt, pc.lastContributionAt as lastContributionAt
      from CommunityContributionDay c
      join Language l on l.id = c.languageId
      join Project r on r.id = c.projectId
      left join r.baseLanguage bl
      left join r.organizationOwner o
      left join ProjectContributor pc on pc.projectId = c.projectId and pc.userId = c.userId
      where c.userId = :userId and l.deletedAt is null and $PUBLIC_PROJECT_VISIBILITY
    """,
  )
  fun findPublicRowsOfUser(userId: Long): List<CommunityProfileRowView>

  @Query(
    """
      select s.project.id as projectId, count(s) as active
      from TranslationSuggestion s
      join s.project r
      left join r.baseLanguage bl
      left join r.organizationOwner o
      where s.author.id = :userId and $LIVE_ACTIVE_S and $PUBLIC_PROJECT_VISIBILITY
      group by s.project.id
    """,
  )
  fun findPublicActiveSuggestionsOfUser(userId: Long): List<ProjectActiveSuggestionsView>

  @Query(
    """
      select distinct c.projectId
      from CommunityContributionDay c
      join Language l on l.id = c.languageId
      join Project r on r.id = c.projectId
      left join r.baseLanguage bl
      left join r.organizationOwner o
      where c.userId = :userId and l.deletedAt is null and $PUBLIC_PROJECT_VISIBILITY
    """,
  )
  fun findPublicProjectIds(
    userId: Long,
    pageable: Pageable,
  ): List<Long>

  companion object {
    const val LIVE_USER_AND_LANGUAGE = "u.deletedAt is null and u.disabledAt is null and l.deletedAt is null"
    const val IN_RANGE = "c.day between :from and :to"
    const val LANGUAGE_FILTER = "(:languageIds is null or c.languageId in :languageIds)"
    const val LANGUAGE_FILTER_S = "(:languageIds is null or s.language.id in :languageIds)"
    const val ACTIVE = "io.tolgee.model.enums.TranslationSuggestionState.ACTIVE"
    const val LIVE_ACTIVE_S = "s.state = $ACTIVE and s.language.deletedAt is null"
    const val TRANSLATION_KEY = "concat(cast(s.key.id as string), ':', cast(s.language.id as string))"
  }
}
