package io.tolgee.configuration

import io.tolgee.configuration.tolgee.SendInBlueProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import sendinblue.ApiClient
import sendinblue.auth.ApiKeyAuth
import sibApi.ContactsApi

@Configuration
class SendInBlueConfiguration(
  private val sendInBlueProperties: SendInBlueProperties,
) {
  @Bean
  fun sendInBlueContactsApi(): ContactsApi? {
    if (sendInBlueProperties.apiKey != null) {
      val client = ApiClient()
      val apiKey = client.getAuthentication("api-key") as ApiKeyAuth
      apiKey.apiKey = sendInBlueProperties.apiKey
      val contactsApi = ContactsApi()
      contactsApi.apiClient = client
      return contactsApi
    }
    return null
  }
}
