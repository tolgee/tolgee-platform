package io.tolgee.unit.cachePurging

import io.tolgee.component.contentDelivery.cachePurging.bunny.BunnyContentDeliveryCachePurging
import io.tolgee.configuration.tolgee.ContentDeliveryBunnyProperties
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.invocation.Invocation
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.net.URI

class BunnyContentStorageConfigCachePurgingTest {
  @Test
  fun `correctly purges`() {
    val config =
      ContentDeliveryBunnyProperties(
        urlPrefix = "https://cdn.example.com",
        apiKey = "token",
      )
    val restTemplateMock: RestTemplate = mock()
    val purging = BunnyContentDeliveryCachePurging(config, restTemplateMock)
    val responseMock: ResponseEntity<*> = Mockito.mock(ResponseEntity::class.java)
    whenever(restTemplateMock.exchange(any<URI>(), any<HttpMethod>(), any(), eq(String::class.java))).doAnswer {
      responseMock as ResponseEntity<String>
    }
    whenever(responseMock.statusCode).thenReturn(HttpStatusCode.valueOf(200))
    val contentDeliveryConfig = mock<ContentDeliveryConfig>()
    whenever(contentDeliveryConfig.slug).thenReturn("fake-slug")

    purging.purgeForPaths(
      contentDeliveryConfig = contentDeliveryConfig,
      paths = (1..15).map { "fake-path-$it" }.toSet(),
    )

    val invocations = Mockito.mockingDetails(restTemplateMock).invocations
    val invocation = invocations.single()
    assertUrl(invocation)
    assertAuthorizationHeader(invocation)
  }

  private fun assertAuthorizationHeader(invocation: Invocation) {
    val httpEntity = getHttpEntity(invocation)
    val headers = httpEntity.headers
    headers["AccessKey"].assert.isEqualTo(listOf("token"))
  }

  private fun getHttpEntity(invocation: Invocation) = invocation.arguments[2] as HttpEntity<*>

  // The first argument must be a URI rather than a String — passing a String would route through
  // RestTemplate's URI template engine and re-encode the already percent-encoded characters in
  // the `url` query parameter, producing a double-encoded URL that Bunny rejects.
  private fun assertUrl(invocation: Invocation) {
    val uri = invocation.arguments[0]
    uri.assert.isInstanceOf(URI::class.java)
    uri.assert.isEqualTo(
      URI("https://api.bunny.net/purge?url=https%3A%2F%2Fcdn.example.com%2Ffake-slug%2F*"),
    )
  }
}
