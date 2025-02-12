package io.tolgee.ee.service

import io.tolgee.component.FrontendUrlProvider
import org.springframework.stereotype.Component

@Component
class AssigneeNotificationService(
  private val frontendUrlProvider: FrontendUrlProvider,
) {
  @Deprecated(
    "Use FrontendUrlProvider.getTaskUrl directly, " +
      "this is kept here only for backwards compatibility with billing.",
  )
  fun getTaskUrl(
    projectId: Long,
    taskId: Long,
  ): String = frontendUrlProvider.getTaskUrl(projectId, taskId)
}
