package io.tolgee.service

import org.springframework.stereotype.Service

@Service
class SlackConfigCleanupServiceOosStub : SlackConfigCleanupService {
  override fun deleteAllOfOrganizationWorkspaces(
    projectId: Long,
    organizationId: Long,
  ) {
    // no-op
  }
}
