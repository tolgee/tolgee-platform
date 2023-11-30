package io.tolgee.configuration

import org.springframework.context.annotation.Bean
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder

@Component
class RestTemplateConfiguration {

  @Bean
  fun restTemplate(): RestTemplate {
    return RestTemplate(
      HttpComponentsClientHttpRequestFactory().apply {
        this.httpClient = HttpClientBuilder.create().disableCookieManagement().useSystemProperties().build()
      }
    )
  }

  private fun RestTemplate.removeXmlConverter(): RestTemplate {
    messageConverters.removeIf { it is MappingJackson2XmlHttpMessageConverter }
    return this
  }

  @Bean(name = ["webhookRestTemplate"])
  fun webhookRestTemplate(): RestTemplate {
    return RestTemplate(getClientHttpRequestFactory()).removeXmlConverter()
  }

  private fun getClientHttpRequestFactory(): SimpleClientHttpRequestFactory {
    val clientHttpRequestFactory = SimpleClientHttpRequestFactory()
    clientHttpRequestFactory.setConnectTimeout(2000)
    clientHttpRequestFactory.setReadTimeout(2000)
    return clientHttpRequestFactory
  }
}