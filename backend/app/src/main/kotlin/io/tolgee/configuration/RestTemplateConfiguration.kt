package io.tolgee.configuration

import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.util.UrlSecurity
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.SystemDefaultDnsResolver
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.Timeout
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.InetAddress
import java.net.UnknownHostException

@Component
class RestTemplateConfiguration {
  @Bean
  @Lazy
  @Primary
  fun restTemplate(): RestTemplate {
    return RestTemplate(
      HttpComponentsClientHttpRequestFactory().apply {
        this.httpClient =
          HttpClientBuilder
            .create()
            .disableCookieManagement()
            .useSystemProperties()
            .build()
      },
    ).removeXmlConverter()
  }

  private fun RestTemplate.removeXmlConverter(): RestTemplate {
    messageConverters.removeIf { it is MappingJackson2XmlHttpMessageConverter }
    return this
  }

  @Bean(name = ["webhookRestTemplate"])
  fun webhookRestTemplate(): RestTemplate {
    return RestTemplate(getClientHttpRequestFactory()).removeXmlConverter()
  }

  /**
   * Fetches from app-controlled hosts. Redirects are disabled — a followed redirect would reach an
   * address nobody validated.
   */
  @Bean(name = ["appsRestTemplate"])
  fun appsRestTemplate(
    urlSecurity: UrlSecurity,
    appsProperties: AppsProperties,
  ): RestTemplate {
    val connectionManager =
      PoolingHttpClientConnectionManagerBuilder
        .create()
        .setDnsResolver(BlockedAddressRejectingDnsResolver(urlSecurity, appsProperties))
        .setDefaultConnectionConfig(
          ConnectionConfig
            .custom()
            .setConnectTimeout(Timeout.ofMilliseconds(appsProperties.requestTimeoutMs))
            .setSocketTimeout(Timeout.ofMilliseconds(appsProperties.requestTimeoutMs))
            .build(),
        ).build()

    val httpClient =
      HttpClientBuilder
        .create()
        .disableCookieManagement()
        .disableRedirectHandling()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(
          RequestConfig
            .custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(appsProperties.requestTimeoutMs))
            .setResponseTimeout(Timeout.ofMilliseconds(appsProperties.requestTimeoutMs))
            .build(),
        ).build()

    return RestTemplate(HttpComponentsClientHttpRequestFactory(httpClient)).removeXmlConverter()
  }

  class BlockedAddressRejectingDnsResolver(
    private val urlSecurity: UrlSecurity,
    private val appsProperties: AppsProperties,
  ) : DnsResolver {
    override fun resolve(host: String): Array<InetAddress> {
      val addresses = SystemDefaultDnsResolver.INSTANCE.resolve(host)
      if (appsProperties.allowLocalAddresses) return addresses
      if (addresses.any { urlSecurity.isBlockedAddress(it) }) {
        throw UnknownHostException("Refusing to connect to a blocked address for host $host")
      }
      return addresses
    }

    override fun resolveCanonicalHostname(host: String): String {
      return SystemDefaultDnsResolver.INSTANCE.resolveCanonicalHostname(host)
    }
  }

  private fun getClientHttpRequestFactory(): SimpleClientHttpRequestFactory {
    val clientHttpRequestFactory = SimpleClientHttpRequestFactory()
    clientHttpRequestFactory.setConnectTimeout(2000)
    clientHttpRequestFactory.setReadTimeout(2000)
    return clientHttpRequestFactory
  }
}
