package io.tolgee.hateoas.organization.slack

import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class WorkspaceModelAssembler : RepresentationModelAssembler<OrganizationSlackWorkspace, WorkspaceModel> {
  override fun toModel(entity: OrganizationSlackWorkspace): WorkspaceModel {
    return WorkspaceModel(
      id = entity.id,
      slackTeamName = entity.slackTeamName,
      slackTeamId = entity.slackTeamId,
    )
  }
}
