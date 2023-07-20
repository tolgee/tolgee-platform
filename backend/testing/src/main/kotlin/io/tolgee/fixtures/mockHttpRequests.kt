package io.tolgee.fixtures

import org.springframework.web.client.RestTemplate

fun mockHttpRequest(restTemplate: RestTemplate, mock: HttpClientMocker.() -> Unit) {
  val httpClientMocker = HttpClientMocker(restTemplate)
  mock(httpClientMocker)
}
