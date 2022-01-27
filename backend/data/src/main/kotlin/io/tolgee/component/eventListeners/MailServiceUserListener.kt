package io.tolgee.component.eventListeners

import io.tolgee.component.MarketingEmailServiceManager
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.user.OnUserCreated
import io.tolgee.events.user.OnUserEmailVerifiedFirst
import io.tolgee.events.user.OnUserUpdated
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MailServiceUserListener(
  private val tolgeeProperties: TolgeeProperties,
  private val marketingEmailServiceManager: MarketingEmailServiceManager
) {

  @EventListener(OnUserCreated::class)
  fun onUserCreated(event: OnUserCreated) {
    if (!tolgeeProperties.authentication.needsEmailVerification) {
      marketingEmailServiceManager.submitNewContact(
        name = event.userAccount.name,
        email = event.userAccount.username
      )
    }
  }

  @EventListener(OnUserEmailVerifiedFirst::class)
  fun onUserEmailVerifiedFirst(event: OnUserEmailVerifiedFirst) {
    marketingEmailServiceManager.submitNewContact(
      name = event.userAccount.name,
      email = event.userAccount.username
    )
  }

  @EventListener(OnUserUpdated::class)
  fun onUserUpdated(event: OnUserUpdated) {
    if (
      event.oldUserAccount.username != event.newUserAccount.username ||
      event.oldUserAccount.name != event.newUserAccount.name
    ) {
      marketingEmailServiceManager.updateContact(
        oldEmail = event.oldUserAccount.username,
        newEmail = event.newUserAccount.username,
        newName = event.newUserAccount.name
      )
    }
  }
}
