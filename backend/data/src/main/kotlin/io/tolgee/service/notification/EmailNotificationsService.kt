package io.tolgee.service.notification

import io.sentry.Sentry
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.notifications.Notification
import io.tolgee.util.I18n
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component

@Component
class EmailNotificationsService(
  private val tolgeeEmailSender: TolgeeEmailSender,
  private val emailComposer: EmailNotificationComposer,
  private val i18n: I18n,
  private val frontendUrlProvider: FrontendUrlProvider,
) : Logging {
  fun sendEmailNotification(notification: Notification) {
    if (notification.user.deletedAt != null || notification.user.disabledAt != null) {
      logger.info(
        "Trying to send an email notification to user ${notification.user.username}, " +
          "but the user has been deleted or disabled, skipping.",
      )
      return
    }

    val subject = emailComposer.composeEmailSubject(notification)
    val text = emailComposer.composeEmailText(notification)
    val params =
      EmailParams(
        to = notification.user.username,
        subject = subject,
        text = i18n.translate("notifications.email.template", text, frontendUrlProvider.getNotificationSettingsUrl()),
      )

    try {
      tolgeeEmailSender.sendEmail(params)
    } catch (e: Exception) {
      logger.error(e.message)
      Sentry.captureException(e)
    }
  }
}
