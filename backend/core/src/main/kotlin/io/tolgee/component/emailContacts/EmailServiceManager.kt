package io.tolgee.component.emailContacts

import org.springframework.scheduling.annotation.Async

interface EmailServiceManager {
  @Async
  fun submitNewContact(
    name: String,
    email: String,
  )

  @Async
  fun updateContact(
    oldEmail: String,
    newEmail: String,
    newName: String,
  )
}
