package io.tolgee.repository.slackIntegration

import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationSlackWorkspaceRepository : JpaRepository<OrganizationSlackWorkspace, Long> {
  fun findBySlackTeamId(teamId: String): OrganizationSlackWorkspace?

  fun findAllByOrganizationId(organizationId: Long): List<OrganizationSlackWorkspace>

  fun findByOrganizationIdAndId(
    organizationId: Long,
    workspaceId: Long,
  ): OrganizationSlackWorkspace?
}
