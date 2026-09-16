package io.tolgee.util

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI

/**
 * Validates URLs to prevent Server-Side Request Forgery (SSRF) attacks.
 * Blocks requests to internal/private network addresses, loopback, and link-local ranges.
 */
@Component
class UrlSecurity(
  private val internalProperties: InternalProperties,
) {
  /**
   * Validates that the given URL is a safe external URL.
   * Throws [BadRequestException] if the URL is malformed, uses a non-HTTP(S) scheme,
   * or the host is a known private/internal address.
   *
   * Skipped when tolgee.internal.disable-url-ssrf-protection is true (E2E tests use localhost URLs).
   */
  fun validateUrl(
    url: String,
    allowLocalAddresses: Boolean = false,
  ) {
    if (internalProperties.disableUrlSsrfProtection) return

    val host = requireHttpHost(url)
    if (allowLocalAddresses) return

    requireNotLocalhostName(host)
    requireNoBlockedAddress(resolve(host))
  }

  /**
   * Resolves the host **once**, validates the resolved addresses, and returns them — so a caller can pin its
   * connection to exactly these, closing the DNS-rebinding gap [validateUrl] leaves open (it resolves, validates and
   * then discards the addresses, letting any later re-resolution answer a different IP).
   *
   * Unlike [validateUrl] this ignores `disable-url-ssrf-protection`: the fetcher relaxing SSRF for dev localhost is a
   * decision it makes itself (passing `allowLocalAddresses`), and it always needs the addresses to pin to.
   */
  fun validateUrlAndResolve(
    url: String,
    allowLocalAddresses: Boolean = false,
  ): List<InetAddress> = resolveAndValidateHost(requireHttpHost(url), allowLocalAddresses)

  /**
   * Resolves a bare host, validates the addresses, and returns them — the host-only half of [validateUrlAndResolve],
   * for a DNS resolver that must both resolve and vet at connect time so a re-resolving HTTP client cannot be pointed
   * at an internal address after the URL passed validation.
   */
  fun resolveAndValidateHost(
    host: String,
    allowLocalAddresses: Boolean = false,
  ): List<InetAddress> {
    val addresses = resolve(host)
    if (!allowLocalAddresses) {
      requireNotLocalhostName(host)
      requireNoBlockedAddress(addresses)
    }
    return addresses.toList()
  }

  private fun requireHttpHost(url: String): String {
    val uri =
      try {
        URI(url)
      } catch (_: Exception) {
        throw BadRequestException(Message.URL_NOT_VALID)
      }

    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") {
      throw BadRequestException(Message.URL_NOT_VALID)
    }

    return uri.host ?: throw BadRequestException(Message.URL_NOT_VALID)
  }

  private fun requireNotLocalhostName(host: String) {
    val lowerHost = host.lowercase()
    if (lowerHost == "localhost" || lowerHost.endsWith(".localhost")) {
      throw BadRequestException(Message.URL_NOT_VALID)
    }
  }

  // IP literals are parsed without a DNS lookup.
  private fun resolve(host: String): Array<InetAddress> {
    val rawHost = host.removeSurrounding("[", "]")
    return try {
      InetAddress.getAllByName(rawHost)
    } catch (_: Exception) {
      throw BadRequestException(Message.URL_NOT_VALID)
    }
  }

  private fun requireNoBlockedAddress(addresses: Array<InetAddress>) {
    for (address in addresses) {
      if (address.isLoopbackAddress ||
        address.isSiteLocalAddress ||
        address.isLinkLocalAddress ||
        address.isAnyLocalAddress ||
        address.isMulticastAddress ||
        isIpv6UniqueLocal(address)
      ) {
        throw BadRequestException(Message.URL_NOT_VALID)
      }
    }
  }

  // IPv6 Unique Local Addresses (fc00::/7) are not covered by isSiteLocalAddress
  private fun isIpv6UniqueLocal(address: InetAddress): Boolean {
    val bytes = address.address
    return bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC
  }
}
