package io.tolgee.repository

import io.tolgee.model.contributor.CommunityContributionDay
import io.tolgee.model.contributor.CommunityContributionDayId
import io.tolgee.model.views.CommunityContributorTotalsView
import io.tolgee.model.views.CommunityDayRowView
import io.tolgee.model.views.CommunityProfileRowView
import io.tolgee.model.views.DayCountsView
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
        sum(c.suggested) as suggested, sum(c.accepted) as accepted, sum(c.declined) as declined
      from CommunityContributionDay c
      join UserAccount u on u.id = c.userId
      join Language l on l.id = c.languageId
      where c.projectId = :projectId and $LIVE_USER_AND_LANGUAGE
      group by c.userId, u.name, u.avatarHash
    """,
    countQuery = """
      select count(distinct c.userId)
      from CommunityContributionDay c
      join UserAccount u on u.id = c.userId
      join Language l on l.id = c.languageId
      where c.projectId = :projectId and $LIVE_USER_AND_LANGUAGE
    """,
  )
  fun findContributorTotals(
    projectId: Long,
    pageable: Pageable,
  ): Page<CommunityContributorTotalsView>

  @Query(
    """
      select c.userId as userId, c.languageId as languageId, c.day as day,
        c.suggested as suggested, c.accepted as accepted, c.declined as declined
      from CommunityContributionDay c
      join Language l on l.id = c.languageId
      where c.projectId = :projectId and c.userId in :userIds and l.deletedAt is null
    """,
  )
  fun findDayRows(
    projectId: Long,
    userIds: Collection<Long>,
  ): List<CommunityDayRowView>

  @Query(
    """
      select c.day as day, sum(c.suggested) as suggested, sum(c.accepted) as accepted, sum(c.declined) as declined
      from CommunityContributionDay c
      join UserAccount u on u.id = c.userId
      join Language l on l.id = c.languageId
      where c.projectId = :projectId and c.day >= :since and $LIVE_USER_AND_LANGUAGE
      group by c.day
    """,
  )
  fun findDailyTotals(
    projectId: Long,
    since: LocalDate,
  ): List<DayCountsView>

  @Query(
    """
      select c.day as day, c.suggested as suggested, c.accepted as accepted, c.declined as declined,
        l.tag as languageTag, l.name as languageName,
        r.id as projectId, r.name as projectName, r.avatarHash as projectAvatarHash
      from CommunityContributionDay c
      join Language l on l.id = c.languageId
      join Project r on r.id = c.projectId
      left join r.baseLanguage bl
      left join r.organizationOwner o
      where c.userId = :userId and l.deletedAt is null and $PUBLIC_PROJECT_VISIBILITY
    """,
  )
  fun findPublicRowsOfUser(userId: Long): List<CommunityProfileRowView>

  companion object {
    const val LIVE_USER_AND_LANGUAGE = "u.deletedAt is null and u.disabledAt is null and l.deletedAt is null"
  }
}
