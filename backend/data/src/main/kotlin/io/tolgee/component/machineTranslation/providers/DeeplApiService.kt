package io.tolgee.component.machineTranslation.providers

import com.fasterxml.jackson.annotation.JsonProperty
import io.tolgee.configuration.tolgee.machineTranslation.DeeplMachineTranslationProperties
import io.tolgee.model.mtServiceConfig.Formality
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class DeeplApiService(
  private val deeplMachineTranslationProperties: DeeplMachineTranslationProperties,
  private val restTemplate: RestTemplate,
) {
  fun translate(
    text: String,
    sourceTag: String,
    targetTag: String,
    formality: Formality,
  ): String? {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

    val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap()
    // Mandatory parameters
    requestBody.add("auth_key", deeplMachineTranslationProperties.authKey)
    requestBody.add("text", text)
    requestBody.add("source_lang", sourceTag.uppercase())
    requestBody.add("target_lang", targetTag.uppercase())
    deeplMachineTranslationProperties.optionalParameters?.forEach {
      requestBody.add(it.key, it.value)
    }
    addFormality(requestBody, formality)

    val response =
      restTemplate.postForEntity<DeeplResponse>(
        apiEndpointFromKey(),
        requestBody,
      )

    return response.body
      ?.translations
      ?.first()
      ?.text
      ?: throw RuntimeException(response.toString())
  }

  private fun addFormality(
    requestBody: MultiValueMap<String, String>,
    formality: Formality,
  ) {
    val deeplFormality =
      when (formality) {
        Formality.FORMAL -> "prefer_more"
        Formality.INFORMAL -> "prefer_less"
        else -> "default"
      }

    return requestBody.add("formality", deeplFormality)
  }

  /**
   * Free API keys are identified by the ':fx' suffix, they also require a different endpoint.
   */
  private fun apiEndpointFromKey(): String {
    if (deeplMachineTranslationProperties.authKey!!.endsWith(":fx")) {
      return "https://api-free.deepl.com/v2/translate"
    }
    return "https://api.deepl.com/v2/translate"
  }

  /**
   * Data structure for mapping the DeepL JSON response objects.
   */
  companion object {
    class DeeplResponse {
      @JsonProperty("translations")
      var translations: List<DeeplTranslation>? = null
    }

    class DeeplTranslation {
      @JsonProperty("detected_source_language")
      var detectedSourceLanguage: String? = null

      @JsonProperty("text")
      var text: String? = null
    }
  }
}
