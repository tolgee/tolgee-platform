package io.tolgee.component.machineTranslation.providers

import com.fasterxml.jackson.annotation.JsonProperty
import io.tolgee.configuration.tolgee.machineTranslation.AzureCognitiveTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.util.LinkedList

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class AzureCognitiveApiService(
  private val azureCognitiveTranslationProperties: AzureCognitiveTranslationProperties,
  private val restTemplate: RestTemplate,
) {
  fun translate(
    text: String,
    sourceTag: String,
    targetTag: String,
  ): String? {
    val headers = HttpHeaders()
    headers.add("Ocp-Apim-Subscription-Key", azureCognitiveTranslationProperties.authKey)
    // Optional when using a single-service Translator Resource
    if (azureCognitiveTranslationProperties.region != null) {
      headers.add("Ocp-Apim-Subscription-Region", azureCognitiveTranslationProperties.region)
    }
    headers.contentType = MediaType.APPLICATION_JSON

    val requestBody: List<AzureCognitiveRequest> = listOf(AzureCognitiveRequest(text))
    val request = HttpEntity(requestBody, headers)

    val response: ResponseEntity<LinkedList<AzureCognitiveResponse>> =
      restTemplate.exchange(
        "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=$sourceTag&to=$targetTag",
        HttpMethod.POST,
        request,
      )

    return response.body
      ?.first
      ?.translations
      ?.first()
      ?.text
      ?: throw RuntimeException(response.toString())
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    class AzureCognitiveRequest(
      text: String,
    ) {
      @JsonProperty("Text")
      var text: String? = text
    }

    class AzureCognitiveResponse {
      var translations: List<AzureCognitiveTranslation>? = null
    }

    class AzureCognitiveTranslation {
      var text: String? = null
      var to: String? = null
    }
  }
}
