package io.tolgee.service.apps.lifecycle

import io.tolgee.component.automations.processors.WebhookSigner
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.util.UrlSecurity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Posts a lifecycle delivery to an app-controlled URL. Shares `appsRestTemplate` with the manifest
 * fetch, so both go through the same SSRF guard: the URL is validated here and every address the
 * client actually connects to is re-checked by that template's DNS resolver, which is what closes
 * DNS rebinding.
 */
@Component
class AppLifecycleHttpClient(
  @Qualifier("appsRestTemplate")
  private val restTemplate: RestTemplate,
  private val urlSecurity: UrlSecurity,
  private val appsProperties: AppsProperties,
  private val webhookSigner: WebhookSigner,
) {
  class DeliveryFailedException(
    message: String,
  ) : RuntimeException(message)

  fun post(
    url: String,
    payload: String,
    signingSecret: String,
  ) {
    validateTarget(url)

    val headers = HttpHeaders()
    @Suppress("UastIncorrectHttpHeaderInspection")
    headers.add(WebhookSigner.SIGNATURE_HEADER, webhookSigner.signatureHeader(payload, signingSecret))
    headers.contentType = MediaType.APPLICATION_JSON

    val response =
      try {
        restTemplate.exchange(URI(url), HttpMethod.POST, HttpEntity(payload, headers), String::class.java)
      } catch (e: Exception) {
        throw DeliveryFailedException(e.message ?: e.javaClass.simpleName)
      }

    if (!response.statusCode.is2xxSuccessful) {
      throw DeliveryFailedException("app responded with status ${response.statusCode.value()}")
    }
  }

  private fun validateTarget(url: String) {
    try {
      urlSecurity.validateUrl(url, allowLocalAddresses = appsProperties.allowLocalAddresses)
    } catch (e: Exception) {
      throw DeliveryFailedException(e.message ?: "delivery URL rejected")
    }
    requireHttps(url)
  }

  /**
   * Credentials travel in the body, so plaintext HTTP is refused. `allowLocalAddresses` is the
   * development switch the manifest fetch already uses, and it is what also permits `http://` here.
   */
  private fun requireHttps(url: String) {
    if (appsProperties.allowLocalAddresses) return
    val scheme = URI(url).scheme?.lowercase()
    if (scheme == "https") return
    throw DeliveryFailedException("lifecycle deliveries require https, got $scheme")
  }
}
