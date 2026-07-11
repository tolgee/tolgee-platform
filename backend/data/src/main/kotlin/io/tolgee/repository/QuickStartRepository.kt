package io.tolgee.repository

import io.tolgee.dtos.queryResults.organization.QuickStartView
import io.tolgee.model.QuickStart
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface QuickStartRepository : JpaRepository<QuickStart, Long> {
  fun findByUserAccountId(userAccountId: Long): QuickStart?

  fun findByUserAccountIdAndOrganizationId(
    userAccountId: Long,
    organizationId: Long,
  ): QuickStart?

  @Query(
    """
    select new io.tolgee.dtos.queryResults.organization.QuickStartView(
      qs.finished,
      qs.completedSteps,
      qs.open
    ) from QuickStart qs
    where qs.userAccount.id = :userAccountId and qs.organization.id = :organizationId
    """,
  )
  fun findView(
    userAccountId: Long,
    organizationId: Long,
  ): QuickStartView?
}
