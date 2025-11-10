package io.tolgee.component.emailContacts

import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.resource.Contact
import com.mailjet.client.resource.Contactdata
import com.mailjet.client.resource.Listrecipient
import io.tolgee.configuration.tolgee.MailjetProperties
import io.tolgee.util.runSentryCatching
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class MailJetEmailServiceManager(
  private val mailjetProperties: MailjetProperties,
) : EmailServiceManager {
  private val client by lazy {
    if (mailjetProperties.apiKey.isNullOrEmpty() || mailjetProperties.secretKey.isNullOrEmpty()) {
      return@lazy null
    }

    val options: ClientOptions =
      ClientOptions
        .builder()
        .apiKey(mailjetProperties.apiKey)
        .apiSecretKey(mailjetProperties.secretKey)
        .build()

    MailjetClient(options)
  }

  @Async
  override fun submitNewContact(
    name: String,
    email: String,
  ) {
    client ?: return
    runSentryCatching {
      val id = createNewContact(email)
      addContactToList(id)
      setContactNameProperty(id, name)
    }
  }

  private fun addContactToList(id: Long) {
    val contactListId = mailjetProperties.contactListId ?: return
    val request =
      MailjetRequest(Listrecipient.resource)
        .property("ContactID", id)
        .property("ListID", contactListId)
    client?.post(request)
  }

  private fun setContactNameProperty(
    id: Long,
    name: String,
  ) {
    val request =
      MailjetRequest(Contactdata.resource, id)
        .property(
          "Data",
          JSONArray().also { jsonArray ->
            jsonArray.put(
              JSONObject().also { jsonObject ->
                jsonObject.put("Name", "name")
                jsonObject.put("Value", name)
              },
            )
          },
        )
    client?.put(request)
  }

  private fun createNewContact(email: String): Long {
    val request =
      MailjetRequest(Contact.resource)
        .property(Contact.EMAIL, email)
    val response = client?.post(request)
    return response?.data.contactId
  }

  private val JSONArray?.contactId: Long
    get() {
      return try {
        (this?.get(0) as? JSONObject)?.get("ID")?.toString()?.toLong()
      } catch (e: Exception) {
        throw IllegalStateException("Mailjet did not return contact id", e)
      } ?: throw IllegalStateException("Mailjet did not return contact id")
    }

  @Async
  override fun updateContact(
    oldEmail: String,
    newEmail: String,
    newName: String,
  ) {
    runSentryCatching {
      client ?: return
      val request = MailjetRequest(Contact.resource, oldEmail)
      val contactResponse = client?.get(request)
      val id = contactResponse?.data.contactId
      val contact =
        MailjetRequest(Contact.resource, id)
          .property(Contact.EMAIL, newEmail)
      client?.put(contact)
      setContactNameProperty(id, newName)
    }
  }
}
