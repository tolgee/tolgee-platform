package io.tolgee.unit.apps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.automations.processors.WebhookSigner
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.fixtures.verifyWebhookSignatureHeader
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.assert
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.util.Date

/**
 * The signature an app has to verify. It must be the same envelope project webhooks already use, so
 * an app author implements one verification and not two.
 */
class AppLifecycleHttpClientTest {
  private val restTemplate = RestTemplate()
  private val server: MockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()

  private val now = Date(1_700_000_000_000)
  private val currentDateProvider =
    mock<CurrentDateProvider> {
      on { date } doReturn now
    }
  private val appsProperties = AppsProperties()
  private val signer = WebhookSigner(currentDateProvider)

  /** The host in these tests does not exist, so address checking has to be off to reach the send. */
  private val client =
    AppLifecycleHttpClient(
      restTemplate,
      UrlSecurity(InternalProperties().apply { disableUrlSsrfProtection = true }),
      appsProperties,
      signer,
    )

  private val guardedClient =
    AppLifecycleHttpClient(restTemplate, UrlSecurity(InternalProperties()), appsProperties, signer)

  private val url = "https://app.example.com"
  private val payload = """{"eventType":"app.installed"}"""
  private val secret = "signing-secret"

  @Test
  fun `signs the payload with the app's secret, in the webhook envelope`() {
    var seenHeader: String? = null
    server
      .expect(requestTo(url))
      .andExpect(header("Content-Type", "application/json"))
      .andRespond { request ->
        seenHeader = request!!.headers.getFirst(WebhookSigner.SIGNATURE_HEADER)
        withSuccess().createResponse(request)
      }

    client.post(url, payload, secret)

    server.verify()
    verifyWebhookSignatureHeader(
      payload = payload,
      sigHeader = seenHeader!!,
      secret = secret,
      tolerance = 0,
      currentTimeInMs = now.time,
    ).assert.isTrue()
  }

  @Test
  fun `treats a non-2xx response as a failure`() {
    server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

    assertThrows<AppLifecycleHttpClient.DeliveryFailedException> { client.post(url, payload, secret) }
  }

  /** Credentials travel in the body, so an app that moved to plaintext HTTP gets nothing. */
  @Test
  fun `refuses to deliver over http`() {
    val thrown =
      assertThrows<AppLifecycleHttpClient.DeliveryFailedException> {
        client.post("http://app.example.com", payload, secret)
      }

    thrown.message.assert.contains("https")
    server.verify()
  }

  @Test
  fun `refuses a target the SSRF guard blocks`() {
    assertThrows<AppLifecycleHttpClient.DeliveryFailedException> {
      guardedClient.post("https://127.0.0.1/hook", payload, secret)
    }

    server.verify()
  }

  @Test
  fun `allows http and local addresses when local addresses are allowed`() {
    appsProperties.allowLocalAddresses = true
    server.expect(requestTo("http://127.0.0.1:3000")).andRespond(withSuccess())

    client.post("http://127.0.0.1:3000", payload, secret)

    server.verify()
  }
}
