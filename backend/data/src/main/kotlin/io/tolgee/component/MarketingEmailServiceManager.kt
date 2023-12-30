package io.tolgee.component

import io.sentry.Sentry
import io.tolgee.configuration.tolgee.SendInBlueProperties
import io.tolgee.util.runSentryCatching
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import sibApi.ContactsApi
import sibModel.CreateContact
import sibModel.UpdateContact

@Component
class MarketingEmailServiceManager(
  private val sendInBlueProperties: SendInBlueProperties,
  private val contactsApi: ContactsApi?,
) {
  @Async
  fun submitNewContact(
    name: String,
    email: String,
  ) {
    runSentryCatching {
      if (contactsApi == null) {
        return
      }
      val createContact = getCreateContactDto(email, name)
      contactsApi.createContact(createContact)
    }
  }

  @Async
  fun updateContact(
    oldEmail: String,
    newEmail: String,
    newName: String,
  ) {
    try {
      if (contactsApi == null) {
        return
      }
      val updateContact = getUpdateContactDto(newEmail, newName)
      contactsApi.updateContact(oldEmail, updateContact)
    } catch (e: Exception) {
      Sentry.captureException(e)
      throw e
    }
  }

  private fun getUpdateContactDto(
    newEmail: String,
    newName: String,
  ): UpdateContact {
    val updateContact = UpdateContact()
    updateContact.attributes = mapOf("EMAIL" to newEmail, "NAME" to newName)
    return updateContact
  }

  private fun getCreateContactDto(
    email: String,
    name: String,
  ): CreateContact {
    val createContact = CreateContact()
    createContact.email = email
    createContact.attributes = mapOf("NAME" to name)
    createContact.listIds = listOf(sendInBlueProperties.listId)
    return createContact
  }
}
