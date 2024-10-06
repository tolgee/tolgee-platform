package io.tolgee.unit.cachePurging

import io.tolgee.component.contentDelivery.cachePurging.cloudflare.CloudflareContentDeliveryCachePurging
import io.tolgee.configuration.tolgee.ContentDeliveryCloudflareProperties
import io.tolgee.fixtures.node
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.assertThatJson
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

class CloudflareContentStorageConfigCachePurgingTest() {
  @Test
  fun `correctly purges`() {
    val config =
      ContentDeliveryCloudflareProperties(
        zoneId = "fake-zone-id",
        urlPrefix = "fake-url-prefix",
        maxFilesPerRequest = 10,
        apiKey = "token",
        origins = "fake-origin,fake-origin2",
      )
    val restTemplateMock: RestTemplate = mock()
    val purging = CloudflareContentDeliveryCachePurging(config, restTemplateMock)
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
    val firstInvocation = invocations.first()
    assertFiles(firstInvocation) {
      isArray
        .hasSize(10)
      node("[0]") {
        node("headers").isEqualTo(
          mapOf(
            "Origin" to "fake-origin",
          ),
        )
        node("url").isEqualTo("fake-url-prefix/fake-slug/fake-path-1")
      }
      node("[1]") {
        node("headers").isEqualTo(
          mapOf(
            "Origin" to "fake-origin2",
          ),
        )
      }
    }
    assertUrl(firstInvocation)
    assertAuthorizationHeader(firstInvocation)

    val invocationList = invocations.toList()
    assertFiles(invocationList[1]) {
      isArray.hasSize(10)
    }
    assertFiles(invocationList[2]) {
      isArray.hasSize(10)
    }
    invocationList.assert.hasSize(3)
  }

  private fun assertFiles(
    invocation: Invocation,
    fn: JsonAssert.() -> Unit,
  ) {
    val httpEntity = getHttpEntity(invocation)
    assertThatJson(httpEntity.body) {
      node("files") {
        fn()
      }
    }
  }

  private fun assertAuthorizationHeader(invocation: Invocation) {
    val httpEntity = getHttpEntity(invocation)
    val headers = httpEntity.headers
    headers["Authorization"].assert.isEqualTo(listOf("Bearer token"))
  }

  private fun getHttpEntity(invocation: Invocation) = invocation.arguments[2] as HttpEntity<*>

  private fun assertUrl(invocation: Invocation) {
    val url = invocation.arguments[0]
    url.assert.isEqualTo(
      "https://api.cloudflare.com/client/v4/zones/fake-zone-id/purge_cache",
    )
  }
}
