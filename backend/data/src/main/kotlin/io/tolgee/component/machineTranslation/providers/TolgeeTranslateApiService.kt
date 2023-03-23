package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.configuration.tolgee.machineTranslation.TolgeeMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.util.*

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TolgeeTranslateApiService(
  private val tolgeeMachineTranslationProperties: TolgeeMachineTranslationProperties,
  private val restTemplate: RestTemplate,
) {

  fun translate(params: TolgeeTranslateParams): String? {
    val headers = HttpHeaders()
    headers.add("Something", null)

    val requestBody: List<TolgeeTranslateRequest> = listOf(TolgeeTranslateRequest(params.text, params.metadata))
    val request = HttpEntity(requestBody, headers)

    val response: ResponseEntity<LinkedList<TolgeeTranslateResponse>> = restTemplate.exchange(
      "${tolgeeMachineTranslationProperties.url}/translate",
      HttpMethod.POST,
      request
    )

    return response.body?.first?.translations?.first()?.text
      ?: throw RuntimeException(response.toString())
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    class TolgeeTranslateParams(
      val text: String,
      val sourceTag: String,
      val targetTag: String,
      val metadata: Metadata?
    )

    class TolgeeTranslateRequest(val text: String, val metadata: Metadata?)
    class TolgeeTranslateResponse {
      var translations: List<TolgeeTranslations>? = null
    }

    class TolgeeTranslations {
      var text: String? = null
      var to: String? = null
    }
  }
}
