package io.tolgee.service.notification

import io.sentry.Sentry
import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.Notification
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component

@Component
class EmailNotificationsService(
  private val tolgeeEmailSender: TolgeeEmailSender,
  private val emailComposer: EmailNotificationComposer,
) : Logging {
  fun sendEmailNotification(notification: Notification) {
    val subject = emailComposer.composeEmailSubject(notification)
    val text = emailComposer.composeEmailText(notification)
    val params =
      EmailParams(
        to = notification.user.username,
        subject = subject,
        text =
          """
            |Hello!👋
            |<br/><br/>
            |$text
            |<br/><br/>
            |Regards,<br/>
            |Tolgee
          """.trimMargin(),
      )

    try {
      tolgeeEmailSender.sendEmail(params)
    } catch (e: Exception) {
      logger.error(e.message)
      Sentry.captureException(e)
    }
  }
}
