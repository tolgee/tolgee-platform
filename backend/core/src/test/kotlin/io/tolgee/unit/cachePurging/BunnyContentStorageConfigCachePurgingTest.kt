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

class BunnyContentStorageConfigCachePurgingTest {
  @Test
  fun `correctly purges`() {
    val config =
      ContentDeliveryBunnyProperties(
        urlPrefix = "fake-url-prefix",
        apiKey = "token",
      )
    val restTemplateMock: RestTemplate = mock()
    val purging = BunnyContentDeliveryCachePurging(config, restTemplateMock)
    val responseMock: ResponseEntity<*> = Mockito.mock(ResponseEntity::class.java)
    whenever(restTemplateMock.exchange(any<String>(), any<HttpMethod>(), any(), eq(String::class.java))).doAnswer {
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

  private fun assertUrl(invocation: Invocation) {
    val url = invocation.arguments[0]
    url.assert.isEqualTo(
      "https://api.bunny.net/purge?url=fake-url-prefix%2Ffake-slug%2F*",
    )
  }
}
