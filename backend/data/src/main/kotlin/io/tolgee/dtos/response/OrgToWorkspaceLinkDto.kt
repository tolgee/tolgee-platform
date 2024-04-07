package io.tolgee.dtos.response

import io.tolgee.model.slackIntegration.OrgToWorkspaceLink

data class OrgToWorkspaceLinkDto(
  val workspaceName: String,
  val author: String,
  val channelName: String,
) {
  companion object {
    fun fromEntity(orgToWorkspaceLink: OrgToWorkspaceLink): OrgToWorkspaceLinkDto {
      return OrgToWorkspaceLinkDto(
        workspaceName = orgToWorkspaceLink.workSpaceName,
        author = orgToWorkspaceLink.author,
        channelName = orgToWorkspaceLink.channelName,
      )
    }
  }
}
