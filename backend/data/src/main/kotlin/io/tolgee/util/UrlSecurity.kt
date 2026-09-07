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
    validateUrlAndResolve(url, allowLocalAddresses)
  }

  /**
   * Same checks as [validateUrl], but returns the addresses that were resolved and validated instead of discarding
   * them. A caller that connects afterward should pin its connection to exactly this array — a second, independent
   * DNS lookup at connect time could return a different (attacker-controlled) address than the one validated here.
   *
   * Returns null when nothing was actually resolved: SSRF protection is disabled, or [allowLocalAddresses] let the
   * host through without a lookup. Both mirror [validateUrl]'s existing short-circuits.
   */
  fun validateUrlAndResolve(
    url: String,
    allowLocalAddresses: Boolean = false,
  ): Array<InetAddress>? {
    if (internalProperties.disableUrlSsrfProtection) return null

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

    val host = uri.host ?: throw BadRequestException(Message.URL_NOT_VALID)

    if (allowLocalAddresses) return null

    val lowerHost = host.lowercase()
    if (lowerHost == "localhost" || lowerHost.endsWith(".localhost")) {
      throw BadRequestException(Message.URL_NOT_VALID)
    }

    // Resolve all addresses (IP literals are parsed without DNS lookup)
    val rawHost = host.removeSurrounding("[", "]")
    val addresses =
      try {
        InetAddress.getAllByName(rawHost)
      } catch (_: Exception) {
        throw BadRequestException(Message.URL_NOT_VALID)
      }

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

    return addresses
  }

  // IPv6 Unique Local Addresses (fc00::/7) are not covered by isSiteLocalAddress
  private fun isIpv6UniqueLocal(address: InetAddress): Boolean {
    val bytes = address.address
    return bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC
  }
}
