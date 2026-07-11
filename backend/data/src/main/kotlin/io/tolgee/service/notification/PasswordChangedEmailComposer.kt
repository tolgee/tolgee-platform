package io.tolgee.service.notification

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.model.notifications.Notification
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class PasswordChangedEmailComposer(
  private val frontendUrlProvider: FrontendUrlProvider,
  private val i18n: I18n,
) : EmailComposer {
  override fun composeEmail(notification: Notification): String =
    """
      |${passwordChangedMessage()}
      |<br/><br/>
      |${checkYourSecuritySettingsFooter()}
    """.trimMargin()

  private fun passwordChangedMessage() = i18n.translate("notifications.email.password-changed")

  private fun checkYourSecuritySettingsFooter() =
    i18n
      .translate("notifications.email.security-settings-link", frontendUrlProvider.getAccountSecurityUrl())
}
