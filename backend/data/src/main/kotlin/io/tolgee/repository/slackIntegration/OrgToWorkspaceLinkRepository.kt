package io.tolgee.repository.slackIntegration

import io.tolgee.model.slackIntegration.OrgToWorkspaceLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrgToWorkspaceLinkRepository : JpaRepository<OrgToWorkspaceLink, Long> {
  fun findByWorkSpace(workSpace: String): OrgToWorkspaceLink?

  fun findByOrganizationId(organizationId: Long): Optional<OrgToWorkspaceLink>
}
