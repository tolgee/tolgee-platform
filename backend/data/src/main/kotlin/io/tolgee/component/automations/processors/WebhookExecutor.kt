package io.tolgee.component.automations.processors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.CurrentDateProvider
import io.tolgee.fixtures.computeHmacSha256
import io.tolgee.model.webhook.WebhookConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class WebhookExecutor(
  @Qualifier("webhookRestTemplate")
  private val restTemplate: RestTemplate,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun signAndExecute(
    config: WebhookConfig,
    data: WebhookRequest,
  ) {
    val stringData = jacksonObjectMapper().writeValueAsString(data)
    val headers = HttpHeaders()
    @Suppress("UastIncorrectHttpHeaderInspection")
    headers.add("Tolgee-Signature", generateSigHeader(stringData, config.webhookSecret))
    headers.contentType = MediaType.APPLICATION_JSON

    val request = HttpEntity(stringData, headers)

    try {
      val responseEntity: ResponseEntity<String> =
        restTemplate.exchange(config.url, HttpMethod.POST, request, String::class.java)
      if (!responseEntity.statusCode.is2xxSuccessful) {
        throw WebhookRespondedWithNon200Status(responseEntity.statusCode, responseEntity.body)
      }
    } catch (e: Exception) {
      throw WebhookExecutionFailed(e)
    }
  }

  private fun generateSigHeader(
    payload: String,
    key: String,
  ): String {
    val timestamp = currentDateProvider.date.time
    val signature = computeHmacSha256(key, "$timestamp.$payload")
    return String.format("""{"timestamp": $timestamp, "signature": "$signature"}""")
  }
}
