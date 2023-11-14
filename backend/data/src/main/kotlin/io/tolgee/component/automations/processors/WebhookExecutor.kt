package io.tolgee.component.automations.processors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.webhook.WebhookConfig
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class WebhookExecutor(
  private val restTemplate: RestTemplate,
  private val currentDateProvider: CurrentDateProvider
) {
  fun signAndExecute(config: WebhookConfig, data: WebhookRequest) {
    val stringData = jacksonObjectMapper().writeValueAsString(data)

    val headers = HttpHeaders()
    @Suppress("UastIncorrectHttpHeaderInspection")
    headers.add("Tolgee-Signature", generateSigHeader(stringData, config.webhookSecret))
    headers.contentType = MediaType.APPLICATION_JSON

    val request = HttpEntity(stringData, headers)

    try {
      val responseEntity: ResponseEntity<Any> =
        restTemplate.exchange(config.url, HttpMethod.POST, request, Any::class.java)
      if (!responseEntity.statusCode.is2xxSuccessful) {
        throw WebhookRespondedWithNon200Status(responseEntity.statusCode, responseEntity.body)
      }
    } catch (e: Exception) {
      throw WebhookExecutionFailed(e)
    }
  }

  private fun generateSigHeader(payload: String, key: String): String {
    val timestamp = currentDateProvider.date.time / 1000
    val payloadToSign = String.format("%d.%s", timestamp, payload)
    val signature = computeHmacSha256(key, payloadToSign)

    return String.format("t=%d,%s=%s", timestamp, "v1", signature)
  }

  fun computeHmacSha256(key: String, message: String): String {
    val hasher = Mac.getInstance("HmacSHA256")
    hasher.init(SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
    val hash = hasher.doFinal(message.toByteArray(StandardCharsets.UTF_8))
    var result = ""
    for (b in hash) {
      result += ((b.toInt() and 0xff) + 0x100).toString(16).substring(1)
    }
    return result
  }
}
