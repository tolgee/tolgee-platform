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

  /**
   * No redirects: a webhook POST carries the project's payload and a `Tolgee-Signature` HMAC over it, and
   * httpclient5 would copy both onto a cross-host redirect the project admin never configured.
   */
  @Bean(name = ["webhookRestTemplate"])
  fun webhookRestTemplate(): RestTemplate =
    ssrfSafeTemplate(
      webhookProperties.allowLocalAddresses,
      Duration.ofSeconds(2),
      followRedirects = false,
    )

  /**
   * No redirects: the token request carries the tenant's `client_secret` and the authorization code, and the
   * response is trusted without checking the `id_token` signature. OpenID Connect Core 3.1.3.7 allows that only
   * while the answer came from the configured token endpoint over TLS, which a followed redirect breaks.
   */
  @Bean(name = ["ssoRestTemplate"])
  fun ssoRestTemplate(): RestTemplate =
    ssrfSafeTemplate(
      tolgeeProperties.authentication.ssoOrganizations.allowLocalAddresses,
      Duration.ofSeconds(10),
      followRedirects = false,
    )

  /**
   * Global SSO endpoints are operator configuration — an on-prem IdP on a private address is the normal case — so
   * this template does not validate the address it connects to. A redirect would then reach an unvalidated host
   * with the secret in hand.
   */
  @Bean(name = ["ssoGlobalRestTemplate"])
  fun ssoGlobalRestTemplate(): RestTemplate =
    ssrfSafeTemplate(allowLocalAddresses = true, Duration.ofSeconds(10), followRedirects = false)

  /**
   * [followRedirects] has no default: every template here carries a secret, and httpclient5 copies method, headers
   * and body onto a 307 or 308, so a new bean must state its answer instead of inheriting a silent one.
   */
  private fun ssrfSafeTemplate(
    allowLocalAddresses: Boolean,
    timeout: Duration,
    followRedirects: Boolean,
  ): RestTemplate =
    RestTemplate(
      ssrfSafeRequestFactoryProvider.create(
        allowLocalAddresses,
        connectTimeout = timeout,
        responseTimeout = timeout,
        followRedirects = followRedirects,
      ),
    ).removeXmlConverter()
}
