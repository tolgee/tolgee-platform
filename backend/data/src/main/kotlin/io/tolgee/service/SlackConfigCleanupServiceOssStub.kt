package io.tolgee.service

import org.springframework.stereotype.Service

@Service
class SlackConfigCleanupServiceOssStub : SlackConfigCleanupService {
  override fun deleteProjectConfigsOnOrganizationWorkspaces(
    projectId: Long,
    organizationId: Long,
  ) {
  }
}
