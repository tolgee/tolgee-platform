package io.tolgee.component.machineTranslation.providers

import io.tolgee.component.machineTranslation.MtValueProvider
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

  fun translate(params: TolgeeTranslateParams): MtValueProvider.MtResult {
    val headers = HttpHeaders()
    headers.add("Something", null)

    val closeItems =
      params.metadata?.closeItems?.map { item -> TolgeeTranslateExample(item.key, item.source, item.target) }
    val examples = params.metadata?.examples?.map { item -> TolgeeTranslateExample(item.key, item.source, item.target) }

    val requestBody = TolgeeTranslateRequest(
      params.text,
      params.keyName,
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

    val costString = response.headers.get("Mt-Credits-Cost")?.singleOrNull()
      ?: throw IllegalStateException("No valid Credits-Cost header in response")
    val cost = costString.toInt()

    return MtValueProvider.MtResult(
      response.body?.output
        ?: throw RuntimeException(response.toString()),
      cost,
      response.body?.contextDescription,
    )
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    class TolgeeTranslateRequest(
      val input: String,
      val keyName: String?,
      val source: String,
      val target: String?,
      val examples: List<TolgeeTranslateExample>?,
      val closeItems: List<TolgeeTranslateExample>?,
    )

    class TolgeeTranslateParams(
      val text: String,
      val keyName: String?,
      val sourceTag: String,
      val targetTag: String,
      val metadata: Metadata?
    )

    class TolgeeTranslateExample(
      var keyName: String,
      var source: String,
      var target: String
    )

    class TolgeeTranslateResponse(val output: String, val contextDescription: String?)
  }
}
