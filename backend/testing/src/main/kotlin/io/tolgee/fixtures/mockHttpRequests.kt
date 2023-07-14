package io.tolgee.fixtures

import org.springframework.web.client.RestTemplate

fun mockHttpRequest(restTemplate: RestTemplate, mock: HttpClientMocker.() -> Unit) {
  val httpClientMOcker = HttpClientMocker(restTemplate)
  mock(httpClientMOcker)
}
