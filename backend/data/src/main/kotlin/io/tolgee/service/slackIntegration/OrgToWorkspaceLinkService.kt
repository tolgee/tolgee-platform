package io.tolgee.service.slackIntegration

import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.slackIntegration.OrgToWorkspaceLink
import io.tolgee.repository.slackIntegration.OrgToWorkspaceLinkRepository
import org.springframework.stereotype.Service
import java.util.*
import kotlin.jvm.optionals.getOrElse

@Service
class OrgToWorkspaceLinkService(
  private val orgToWorkspaceLinkRepository: OrgToWorkspaceLinkRepository,
) {
  fun getByWorkSpace(workSpace: String): OrgToWorkspaceLink? {
    return orgToWorkspaceLinkRepository.findByWorkSpace(workSpace)
  }

  fun findByOrgIdOptional(id: Long): Optional<OrgToWorkspaceLink> {
    return orgToWorkspaceLinkRepository.findByOrganizationId(id)
  }

  fun delete(id: Long) {
    val orgToWorkspaceLink = findByOrgIdOptional(id).getOrElse { throw NotFoundException() }
    orgToWorkspaceLinkRepository.delete(orgToWorkspaceLink)
  }

  fun ifWorkSpaceLinked(workSpace: String) = getByWorkSpace(workSpace) != null

  fun save(
    organization: Organization,
    workSpace: String,
    channelName: String,
    author: String,
    teamDomain: String,
  ): OrgToWorkspaceLink {
    val orgToWorkspaceLink = OrgToWorkspaceLink()
    orgToWorkspaceLink.organization = organization
    orgToWorkspaceLink.workSpace = workSpace
    orgToWorkspaceLink.channelName = channelName
    orgToWorkspaceLink.author = author
    orgToWorkspaceLink.workSpaceName = teamDomain
    return orgToWorkspaceLinkRepository.save(orgToWorkspaceLink)
  }

  fun save(orgToWorkspaceLink: OrgToWorkspaceLink): OrgToWorkspaceLink {
    return orgToWorkspaceLinkRepository.save(orgToWorkspaceLink)
  }
}
