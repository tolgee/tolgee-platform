package io.tolgee.component.eventListeners

import io.tolgee.component.emailContacts.EmailServiceManager
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.user.OnUserCreated
import io.tolgee.events.user.OnUserEmailVerifiedFirst
import io.tolgee.events.user.OnUserUpdated
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class MailServiceUserListener(
  private val tolgeeProperties: TolgeeProperties,
  private val emailServiceManagers: List<EmailServiceManager>,
) {
  @TransactionalEventListener(OnUserCreated::class)
  fun onUserCreated(event: OnUserCreated) {
    if (!tolgeeProperties.authentication.needsEmailVerification || event.userAccount.emailVerification == null) {
      emailServiceManagers.forEach {
        it.submitNewContact(
          name = event.userAccount.name,
          email = event.userAccount.username,
        )
      }
    }
  }

  @TransactionalEventListener(OnUserEmailVerifiedFirst::class)
  fun onUserEmailVerifiedFirst(event: OnUserEmailVerifiedFirst) {
    emailServiceManagers.forEach {
      it.submitNewContact(
        name = event.userAccount.name,
        email = event.userAccount.username,
      )
    }
  }

  @TransactionalEventListener(OnUserUpdated::class)
  fun onUserUpdated(event: OnUserUpdated) {
    if (
      event.oldUserAccount.username != event.newUserAccount.username ||
      event.oldUserAccount.name != event.newUserAccount.name
    ) {
      emailServiceManagers.forEach {
        it.updateContact(
          oldEmail = event.oldUserAccount.username,
          newEmail = event.newUserAccount.username,
          newName = event.newUserAccount.name,
        )
      }
    }
  }
}
