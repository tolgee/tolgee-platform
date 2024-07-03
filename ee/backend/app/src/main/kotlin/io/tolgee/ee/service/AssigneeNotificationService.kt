package io.tolgee.ee.service

import io.sentry.Sentry
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.UserAccount
import io.tolgee.model.task.Task
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component

@Component
class AssigneeNotificationService(
  private val tolgeeEmailSender: TolgeeEmailSender,
  private val frontendUrlProvider: FrontendUrlProvider,
) : Logging {
  fun notifyNewAssignee(
    user: UserAccount,
    task: Task,
  ) {
    val url = "${frontendUrlProvider.url}/projects/${task.project.id}/task?number=${task.number}&detail=true"

    val params =
      EmailParams(
        to = user.username,
        subject = "New task assignment",
        text =
          """
          Hello! 👋<br/><br/>          
          You've been assigned to task <b>${task.name} #${task.number} (${task.language.name})</b>:<br/>
          <a href="$url">$url</a><br/><br/>
          
          Regards,<br/>
          Tolgee
          """.trimIndent(),
      )

    try {
      tolgeeEmailSender.sendEmail(params)
    } catch (e: Exception) {
      logger.error(e.message)
      Sentry.captureException(e)
    }
  }
}
