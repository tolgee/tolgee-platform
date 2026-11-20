package io.tolgee.util

import io.tolgee.configuration.tolgee.InternalProperties
import org.apache.hc.client5.http.DnsResolver
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Builds the Apache HttpClient 5 [DnsResolver] shared by every SSRF-sensitive client that pins its connection to the
 * addresses [UrlSecurity] has vetted. Resolving and validating in one step closes the DNS-rebinding gap a
 * validate-then-discard check leaves open: the host is resolved once and the connection goes to exactly those
 * addresses. [SsrfSafeRequestFactoryProvider] (webhooks, SSO, custom LLM endpoints) and the S3 content-storage client
 * both resolve through this one implementation.
 */
@Component
class SsrfSafeDnsResolver(
  private val urlSecurity: UrlSecurity,
  private val internalProperties: InternalProperties,
) : Logging {
  /**
   * A resolver that resolves [host] once and refuses any blocked address, unless [allowLocalAddresses], the
   * `tolgee.internal.disable-url-ssrf-protection` dev switch, or [skipHost] exempts it. On refusal it throws
   * [UnknownHostException], so the connection fails before a socket is opened.
   */
  internal fun create(
    allowLocalAddresses: Boolean,
    skipHost: (String) -> Boolean = { false },
  ): DnsResolver =
    object : DnsResolver {
      override fun resolve(host: String): Array<InetAddress> =
        try {
          val skip = allowLocalAddresses || internalProperties.disableUrlSsrfProtection || skipHost(host)
          urlSecurity.resolveAndValidateHost(host, skip).toTypedArray()
        } catch (e: Exception) {
          logger.warn("Refusing an outbound connection to {}: {}", host, e.message)
          throw UnknownHostException(host).apply { initCause(e) }
        }

      override fun resolveCanonicalHostname(host: String): String = host
    }
}
