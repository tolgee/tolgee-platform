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

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TolgeeTranslateApiService(
  private val tolgeeMachineTranslationProperties: TolgeeMachineTranslationProperties,
  private val restTemplate: RestTemplate
) {

  fun translate(params: TolgeeTranslateParams): String? {
    val headers = HttpHeaders()
    headers.add("Something", null)

    val closeItems =
      params.metadata?.closeItems?.map { item -> TolgeeTranslateExample(item.key, item.source, item.target) }
    val examples = params.metadata?.examples?.map { item -> TolgeeTranslateExample(item.key, item.source, item.target) }

    val requestBody = TolgeeTranslateRequest(
      params.text,
      params.metadata?.keyName,
      params.metadata?.keyNamespace,
      params.sourceTag,
      params.targetTag,
      examples,
      closeItems
    )
    val request = HttpEntity(requestBody, headers)

    val response: ResponseEntity<TolgeeTranslateResponse> = restTemplate.exchange(
      "${tolgeeMachineTranslationProperties.url}/api/openai/translate",
      HttpMethod.POST,
      request
    )

    return response.body?.output
      ?: throw RuntimeException(response.toString())
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    class TolgeeTranslateRequest(
      val input: String,
      val keyName: String?,
      val keyNamespace: String?,
      val source: String,
      val target: String?,
      val examples: List<TolgeeTranslateExample>?,
      val closeItems: List<TolgeeTranslateExample>?,
    )

    class TolgeeTranslateParams(
      val text: String,
      val sourceTag: String,
      val targetTag: String,
      val metadata: Metadata?
    )

    class TolgeeTranslateExample(
      var keyName: String,
      var source: String,
      var target: String
    )

    class TolgeeTranslateResponse(val output: String)
  }
}
