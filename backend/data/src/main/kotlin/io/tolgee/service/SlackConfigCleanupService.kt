package io.tolgee.service

interface SlackConfigCleanupService {
  fun deleteProjectConfigsOnOrganizationWorkspaces(
    projectId: Long,
    organizationId: Long,
  )
}
