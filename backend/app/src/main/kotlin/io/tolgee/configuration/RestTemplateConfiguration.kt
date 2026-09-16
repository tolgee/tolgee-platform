package io.tolgee.configuration

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.WebhookProperties
import io.tolgee.util.SsrfSafeRequestFactoryProvider
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Component
class RestTemplateConfiguration(
  private val ssrfSafeRequestFactoryProvider: SsrfSafeRequestFactoryProvider,
  private val webhookProperties: WebhookProperties,
  private val tolgeeProperties: TolgeeProperties,
) {
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
    messageConverters.removeIf { it is JacksonXmlHttpMessageConverter }
    return this
  }

  // A webhook URL is user-configured, so the delivery connection validates and pins DNS (allowing local addresses only
  // where the deployment opted in for self-hosted testing) rather than trusting the address a re-resolve returns.
  @Bean(name = ["webhookRestTemplate"])
  fun webhookRestTemplate(): RestTemplate {
    val factory =
      ssrfSafeRequestFactoryProvider.create(
        allowLocalAddresses = webhookProperties.allowLocalAddresses,
        connectTimeout = Duration.ofSeconds(2),
        responseTimeout = Duration.ofSeconds(2),
      )
    return RestTemplate(factory).removeXmlConverter()
  }

  // An SSO tenant's token endpoint is admin-configured, so the code-exchange connection validates and pins DNS the
  // same way, honouring the sso-organizations local-address allowance for on-prem identity providers.
  @Bean(name = ["ssoRestTemplate"])
  fun ssoRestTemplate(): RestTemplate {
    val factory =
      ssrfSafeRequestFactoryProvider.create(
        allowLocalAddresses = tolgeeProperties.authentication.ssoOrganizations.allowLocalAddresses,
        connectTimeout = Duration.ofSeconds(10),
        responseTimeout = Duration.ofSeconds(10),
      )
    return RestTemplate(factory).removeXmlConverter()
  }
}
