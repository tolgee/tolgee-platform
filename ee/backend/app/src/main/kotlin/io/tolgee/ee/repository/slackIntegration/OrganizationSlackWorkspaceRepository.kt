package io.tolgee.ee.repository.slackIntegration

import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface OrganizationSlackWorkspaceRepository : JpaRepository<OrganizationSlackWorkspace, Long> {
  fun findBySlackTeamId(teamId: String): OrganizationSlackWorkspace?

  fun findAllByOrganizationId(organizationId: Long): List<OrganizationSlackWorkspace>

  fun findByOrganizationIdAndId(
    organizationId: Long,
    workspaceId: Long,
  ): OrganizationSlackWorkspace?

  fun findByOrganizationIdAndSlackTeamId(
    organizationId: Long,
    teamId: String,
  ): OrganizationSlackWorkspace?

  @Query(
    """
    from OrganizationSlackWorkspace osw
    where osw.id = :id
    """,
  )
  fun find(id: Long): OrganizationSlackWorkspace?
}
