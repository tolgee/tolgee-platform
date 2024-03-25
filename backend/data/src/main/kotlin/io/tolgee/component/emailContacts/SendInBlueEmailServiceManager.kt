package io.tolgee.component.emailContacts

import io.sentry.Sentry
import io.tolgee.configuration.tolgee.SendInBlueProperties
import io.tolgee.util.runSentryCatching
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import sendinblue.ApiClient
import sendinblue.auth.ApiKeyAuth
import sibApi.ContactsApi
import sibModel.CreateContact
import sibModel.UpdateContact

@Component
class SendInBlueEmailServiceManager(
  private val sendInBlueProperties: SendInBlueProperties,
) : EmailServiceManager {
  private val contactsApi by lazy {
    if (sendInBlueProperties.apiKey != null) {
      val client = ApiClient()
      val apiKey = client.getAuthentication("api-key") as ApiKeyAuth
      apiKey.apiKey = sendInBlueProperties.apiKey
      val contactsApi = ContactsApi()
      contactsApi.apiClient = client
      return@lazy contactsApi
    }
    return@lazy null
  }

  @Async
  override fun submitNewContact(
    name: String,
    email: String,
  ) {
    runSentryCatching {
      contactsApi ?: return
      val createContact = getCreateContactDto(email, name)
      contactsApi?.createContact(createContact)
    }
  }

  @Async
  override fun updateContact(
    oldEmail: String,
    newEmail: String,
    newName: String,
  ) {
    try {
      contactsApi ?: return
      val updateContact = getUpdateContactDto(newEmail, newName)
      contactsApi?.updateContact(oldEmail, updateContact)
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
