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

    if (allowLocalAddresses) return

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
      if (isBlockedAddress(address)) {
        throw BadRequestException(Message.URL_NOT_VALID)
      }
    }
  }

  /**
   * [validateUrl] resolves the host itself, but an HTTP client resolves it again when it connects —
   * a host serving low-TTL DNS can answer the two lookups differently (DNS rebinding). Clients that
   * fetch app-controlled URLs must therefore re-check the address they actually connect to.
   */
  fun isBlockedAddress(address: InetAddress): Boolean {
    if (internalProperties.disableUrlSsrfProtection) return false
    return address.isLoopbackAddress ||
      address.isSiteLocalAddress ||
      address.isLinkLocalAddress ||
      address.isAnyLocalAddress ||
      address.isMulticastAddress ||
      isIpv6UniqueLocal(address) ||
      isReservedIpv4(address)
  }

  /**
   * Ranges [InetAddress] has no predicate for. `isAnyLocalAddress` only matches `0.0.0.0` itself,
   * but all of `0.0.0.0/8` is a loopback alias on Linux (`0.1.2.3` reaches `127.0.0.1`), so the
   * whole block is a reachable SSRF bypass. The rest are never legitimate app hosts: CGNAT
   * 100.64.0.0/10 (Alibaba/Tencent cloud metadata endpoints and every Tailscale node live there),
   * 192.0.0.0/24, benchmarking 198.18.0.0/15, 6to4-relay anycast 192.88.99.0/24, the three
   * documentation ranges, and reserved 240.0.0.0/4 including the broadcast address.
   */
  private fun isReservedIpv4(address: InetAddress): Boolean {
    val bytes = address.address
    if (bytes.size != 4) return false
    val first = bytes[0].toInt() and 0xFF
    val second = bytes[1].toInt() and 0xFF
    val third = bytes[2].toInt() and 0xFF
    if (first == 0) return true
    if (first == 100 && second in 64..127) return true
    if (first == 192 && second == 0 && third == 0) return true
    if (first == 192 && second == 0 && third == 2) return true
    if (first == 192 && second == 88 && third == 99) return true
    if (first == 198 && second in 18..19) return true
    if (first == 198 && second == 51 && third == 100) return true
    if (first == 203 && second == 0 && third == 113) return true
    return first >= 240
  }

  // IPv6 Unique Local Addresses (fc00::/7) are not covered by isSiteLocalAddress
  private fun isIpv6UniqueLocal(address: InetAddress): Boolean {
    val bytes = address.address
    return bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC
  }
}
