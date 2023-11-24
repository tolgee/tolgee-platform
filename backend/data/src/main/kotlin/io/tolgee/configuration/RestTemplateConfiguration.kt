package io.tolgee.configuration

import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class RestTemplateConfiguration {

  @Bean
  @Primary
  fun restTemplate(): RestTemplate {
    return RestTemplate(
      HttpComponentsClientHttpRequestFactory().apply {
        this.httpClient = HttpClientBuilder.create().disableCookieManagement().useSystemProperties().build()
      }
    )
  }

  @Bean(name = ["webhookRestTemplate"])
  fun webhookRestTemplate(): RestTemplate {
    return RestTemplate(getClientHttpRequestFactory())
  }

  private fun getClientHttpRequestFactory(): SimpleClientHttpRequestFactory {
    val clientHttpRequestFactory = SimpleClientHttpRequestFactory()
    clientHttpRequestFactory.setConnectTimeout(2000)
    clientHttpRequestFactory.setReadTimeout(2000)
    return clientHttpRequestFactory
  }
}
