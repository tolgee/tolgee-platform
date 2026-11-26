package io.tolgee.service

interface SlackConfigCleanupService {
  fun deleteAllOfOrganizationWorkspaces(
    projectId: Long,
    organizationId: Long,
  )
}
