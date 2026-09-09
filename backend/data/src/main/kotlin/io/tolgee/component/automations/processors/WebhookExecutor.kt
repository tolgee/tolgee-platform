package io.tolgee.component.automations.processors

import io.tolgee.model.webhook.WebhookConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class WebhookExecutor(
  @Qualifier("webhookRestTemplate")
  private val restTemplate: RestTemplate,
  private val webhookSigner: WebhookSigner,
) {
  fun signAndExecute(
    config: WebhookConfig,
    data: WebhookRequest,
  ) {
    val stringData = jacksonObjectMapper().writeValueAsString(data)
    val headers = HttpHeaders()
    @Suppress("UastIncorrectHttpHeaderInspection")
    headers.add(WebhookSigner.SIGNATURE_HEADER, webhookSigner.signatureHeader(stringData, config.webhookSecret))
    headers.contentType = MediaType.APPLICATION_JSON

    val request = HttpEntity(stringData, headers)

    try {
      // Treat as already-encoded only when every '%' starts a valid percent-encoded triplet.
      val fullyEncoded =
        config.url.contains("%") &&
          Regex("%(?![0-9A-Fa-f]{2})").find(config.url) == null
      val uri = UriComponentsBuilder.fromUriString(config.url).build(fullyEncoded).toUri()

      val responseEntity: ResponseEntity<String> =
        restTemplate.exchange(uri, HttpMethod.POST, request, String::class.java)
      if (!responseEntity.statusCode.is2xxSuccessful) {
        throw WebhookRespondedWithNon200Status(responseEntity.statusCode, responseEntity.body)
      }
    } catch (e: Exception) {
      throw WebhookExecutionFailed(e)
    }
  }
}
