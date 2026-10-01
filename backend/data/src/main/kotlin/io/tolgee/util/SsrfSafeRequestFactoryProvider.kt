/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.util

import io.tolgee.configuration.tolgee.InternalProperties
import jakarta.annotation.PreDestroy
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.HttpRoute
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner
import org.apache.hc.client5.http.routing.HttpRoutePlanner
import org.apache.hc.core5.http.HttpException
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.util.Timeout
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Builds a [ClientHttpRequestFactory] that pins DNS, per [UrlSecurity.validateUrlAndResolve], for every request an
 * SSRF-sensitive [RestTemplate] makes to a user-configured URL (webhooks, custom LLM endpoints, SSO token endpoints).
 *
 * The pin is defeated by an egress proxy, which resolves the target host itself — the proxy, not this class, is then
 * the control on where a request may reach. Tolgee honours the JVM's proxy system properties here anyway, because
 * every one of these paths did before it existed and an operator who configured a proxy expects it to be used.
 */
@Component
class SsrfSafeRequestFactoryProvider(
  private val urlSecurity: UrlSecurity,
  private val internalProperties: InternalProperties,
) : Logging {
  private data class ClientKey(
    val allowLocalAddresses: Boolean,
    val connectTimeoutMs: Long,
    val responseTimeoutMs: Long,
    val followRedirects: Boolean,
  )

  private val clients = ConcurrentHashMap<ClientKey, CloseableHttpClient>()

  @PreDestroy
  fun shutdown() {
    clients.values.forEach { runCatching { it.close() } }
    clients.clear()
  }

  fun create(
    allowLocalAddresses: Boolean,
    connectTimeout: Duration,
    responseTimeout: Duration,
    followRedirects: Boolean = false,
  ): ClientHttpRequestFactory {
    val key = ClientKey(allowLocalAddresses, connectTimeout.toMillis(), responseTimeout.toMillis(), followRedirects)
    val client =
      clients.computeIfAbsent(key) {
        ssrfSafeClientBuilder(
          validatingResolver(allowLocalAddresses),
          connectTimeout,
          responseTimeout,
          followRedirects = followRedirects,
        ).build()
      }
    return HttpComponentsClientHttpRequestFactory(client)
  }

  private fun validatingResolver(allowLocalAddresses: Boolean): DnsResolver =
    object : DnsResolver {
      override fun resolve(host: String): Array<InetAddress> =
        try {
          val skip = allowLocalAddresses || skipValidation || isOperatorProxy(host)
          urlSecurity.resolveAndValidateHost(host, skip).toTypedArray()
        } catch (e: Exception) {
          logger.warn("Refusing an outbound connection to {}: {}", host, e.message)
          throw UnknownHostException(host).apply { initCause(e) }
        }

      override fun resolveCanonicalHostname(host: String): String = host
    }

  private val skipValidation: Boolean get() = internalProperties.disableUrlSsrfProtection

  internal fun pooledClientCount(): Int = clients.size

  companion object {
    /** httpclient5 defaults to 5 per route and 25 total. */
    const val MAX_CONNECTIONS_PER_ROUTE = 50
    const val MAX_CONNECTIONS_TOTAL = 200

    /**
     * `useSystemProperties()` routes through the HTTP proxies of [java.net.ProxySelector] only. A SOCKS proxy never
     * becomes a route, so `socksProxyHost` must not be added here: that host would be exempted from the address
     * rules while no request ever goes to it.
     */
    private val PROXY_HOST_PROPERTIES = listOf("http.proxyHost", "https.proxyHost")

    /**
     * Whether [host] is the egress proxy the operator configured. When the JVM proxy properties are set, httpclient5
     * connects to the proxy instead of the target, so the resolver is handed the *proxy's* host name — normally a
     * private one, which the address rules would refuse, taking every webhook, SSO and LLM call down with it.
     *
     * Only the proxy hop is exempt. The resolver cannot tell that hop from a direct request whose target merely
     * carries the same name, which `http.nonProxyHosts` or an https URL under an http-only proxy would send straight
     * out; [ProxyHopOnlyRoutePlanner] refuses those before the resolver sees them.
     */
    private fun isOperatorProxy(host: String): Boolean {
      if (host.isBlank()) return false
      return PROXY_HOST_PROPERTIES.any { host.equals(System.getProperty(it)?.trim(), ignoreCase = true) }
    }

    private object ProxyHopOnlyRoutePlanner : HttpRoutePlanner {
      private val system = SystemDefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE, null)

      override fun determineRoute(
        target: HttpHost,
        request: HttpRequest,
        context: HttpContext,
      ): HttpRoute = refusingDirectRouteToProxy(target, system.determineRoute(target, request, context))

      override fun determineRoute(
        target: HttpHost,
        context: HttpContext,
      ): HttpRoute = refusingDirectRouteToProxy(target, system.determineRoute(target, context))

      private fun refusingDirectRouteToProxy(
        target: HttpHost,
        route: HttpRoute,
      ): HttpRoute {
        if (route.proxyHost == null && isOperatorProxy(target.hostName)) {
          throw HttpException("Refusing a direct connection to the egress proxy host ${target.hostName}")
        }
        return route
      }
    }

    fun ssrfSafeClientBuilder(
      dnsResolver: DnsResolver,
      connectTimeout: Duration,
      responseTimeout: Duration,
      useSystemProperties: Boolean = true,
      followRedirects: Boolean = false,
    ): HttpClientBuilder {
      val requestConfig =
        RequestConfig
          .custom()
          .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
          .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_LEASE_TIMEOUT.toMillis()))
          .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout.toMillis()))
          .setRedirectsEnabled(followRedirects)
          .build()
      val connectionManager =
        PoolingHttpClientConnectionManagerBuilder
          .create()
          .setDnsResolver(dnsResolver)
          .setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
          .setMaxConnTotal(MAX_CONNECTIONS_TOTAL)
          .build()
      val builder =
        HttpClients
          .custom()
          .setConnectionManager(connectionManager)
          // One client is shared by every tenant's outbound calls for the life of the process, so a cookie jar here
          // would carry one tenant's Set-Cookie onto another's request to the same domain, and nothing evicts it.
          .disableCookieManagement()
          // httpclient5 honours a target's `Retry-After` with an unbounded Thread.sleep on the calling thread, and
          // the response timeouts above bound a socket read, not the exec. A hostile endpoint answering
          // `503 Retry-After: 86400` would otherwise park the caller for a day.
          .disableAutomaticRetries()
          .setDefaultRequestConfig(requestConfig)
      if (!followRedirects) builder.disableRedirectHandling()
      if (useSystemProperties) builder.useSystemProperties().setRoutePlanner(ProxyHopOnlyRoutePlanner)
      return builder
    }

    private val CONNECTION_LEASE_TIMEOUT = Duration.ofSeconds(30)
  }
}
