package io.tolgee.service.notification

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.model.notifications.Notification
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class BatchJobEmailComposer(
  private val frontendUrlProvider: FrontendUrlProvider,
  private val i18n: I18n,
) : EmailComposer {
  override fun composeEmail(notification: Notification): String {
    val batchJob =
      notification.linkedBatchJob
        ?: throw IllegalStateException(
          "Notification of type ${notification.type} must contain linkedBatchJob.",
        )
    val projectId = notification.project?.id
    val projectLink =
      if (projectId != null) {
        "<a href=\"${frontendUrlProvider.getProjectUrl(projectId)}\">" +
          "${i18n.translate("notifications.email.view-in-tolgee-link")}</a>"
      } else {
        ""
      }

    return """
        |${i18n.translate("notifications.email.batch-job-header.BATCH_JOB_FINISHED", batchJob.type.name)}
        |<br/><br/>
        |$projectLink
      """.trimMargin()
  }
}
