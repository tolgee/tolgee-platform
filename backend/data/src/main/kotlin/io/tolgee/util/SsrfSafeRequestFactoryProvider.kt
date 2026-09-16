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

import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.Timeout
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration

/**
 * Builds a [ClientHttpRequestFactory] that validates and pins DNS for every request an SSRF-sensitive [RestTemplate]
 * makes to a user-configured URL (webhooks, custom LLM endpoints, SSO token endpoints).
 *
 * The pin closes DNS rebinding: the host is resolved **once**, by [UrlSecurity] inside the client's own DNS resolver,
 * and the connection uses exactly that result — there is no second, unvalidated lookup at connect time for a zero-TTL
 * attacker to answer with an internal address. Redirects are disabled so a validated host cannot 302 the request to
 * an unvalidated one.
 */
@Component
class SsrfSafeRequestFactoryProvider(
  private val urlSecurity: UrlSecurity,
) {
  fun create(
    allowLocalAddresses: Boolean,
    connectTimeout: Duration,
    responseTimeout: Duration,
  ): ClientHttpRequestFactory {
    val validatingResolver =
      object : DnsResolver {
        override fun resolve(host: String): Array<InetAddress> =
          try {
            urlSecurity.resolveAndValidateHost(host, allowLocalAddresses).toTypedArray()
          } catch (e: Exception) {
            // A blocked or unresolvable host must fail the connection, not surface as some other error type.
            throw UnknownHostException(host).apply { initCause(e) }
          }

        override fun resolveCanonicalHostname(host: String): String = host
      }
    val connectionManager =
      PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(validatingResolver).build()
    val requestConfig =
      RequestConfig
        .custom()
        .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
        .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()))
        .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout.toMillis()))
        .setRedirectsEnabled(false)
        .build()
    val client =
      HttpClients
        .custom()
        .setConnectionManager(connectionManager)
        .disableRedirectHandling()
        .setDefaultRequestConfig(requestConfig)
        .build()
    return HttpComponentsClientHttpRequestFactory(client)
  }
}
