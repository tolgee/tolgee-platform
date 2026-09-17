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

  /** The host-only half of [validateUrlAndResolve], for a DNS resolver that resolves and vets at connect time. */
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
      if (isBlockedRange(address) || isIpv6UniqueLocal(address) || disguisesInternalIpv4(address)) {
        throw BadRequestException(Message.URL_NOT_VALID)
      }
    }
  }

  /** The ranges refused whether an address names one directly or an IPv6 address embeds one: add new ones here. */
  private fun isBlockedRange(address: InetAddress): Boolean =
    address.isLoopbackAddress ||
      address.isSiteLocalAddress ||
      address.isLinkLocalAddress ||
      address.isAnyLocalAddress ||
      address.isMulticastAddress ||
      isIpv4Reserved(address)

  // IPv6 Unique Local Addresses (fc00::/7) are not covered by isSiteLocalAddress
  private fun isIpv6UniqueLocal(address: InetAddress): Boolean {
    val bytes = address.address
    return bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC
  }

  /**
   * Ranges the JDK's own predicates do not cover. 100.64/10 is where Kubernetes pod networks and Tailscale live,
   * so it reaches a cluster's internal services as readily as 10/8 does.
   */
  private fun isIpv4Reserved(address: InetAddress): Boolean {
    val bytes = address.address
    if (bytes.size != 4) return false
    val first = bytes[0].toInt() and 0xFF
    val second = bytes[1].toInt() and 0xFF
    val third = bytes[2].toInt() and 0xFF
    if (first == 100 && second in 64..127) return true // RFC 6598 shared address space
    if (first == 192 && second == 0 && third == 0) return true // RFC 6890 IETF protocol
    if (first == 198 && second in 18..19) return true // RFC 2544 benchmarking
    if (first == 192 && second == 88 && third == 99) return true // RFC 7526 6to4 relay anycast
    return first >= 240 // RFC 1112 reserved, including 255.255.255.255
  }

  /**
   * Whether an IPv6 address carries an *internal* IPv4 address inside it: `::a.b.c.d`, `::ffff:a.b.c.d`, 6to4 and
   * the NAT64 well-known prefix all reach the embedded IPv4 address, while every `isLoopbackAddress`-style
   * predicate reads false. A disguised but externally routable address is not refused here.
   * The JDK already maps `::ffff:a.b.c.d` back to an Inet4Address, so only the other two need naming here.
   */
  private fun disguisesInternalIpv4(address: InetAddress): Boolean {
    val bytes = address.address
    if (bytes.size != 16) return false
    if (isNat64Prefix(bytes)) return nat64MustBeRefused(bytes)
    // 6to4 (2002::/16, RFC 3056) carries the IPv4 straight after the prefix.
    if ((bytes[0].toInt() and 0xFF) == 0x20 && (bytes[1].toInt() and 0xFF) == 0x02) {
      return isInternalIpv4(bytes.copyOfRange(2, 6))
    }
    // ::a.b.c.d, ::ffff:a.b.c.d and the RFC 2765 translated form ::ffff:0:a.b.c.d all carry it in the last four
    // bytes. The unspecified address and ::1 are caught by the JDK's own predicates before this runs.
    if ((0..7).all { bytes[it].toInt() == 0 }) return isInternalIpv4(bytes.copyOfRange(12, 16))
    return false
  }

  /**
   * Whether a NAT64 address has to be turned away - either because the IPv4 it embeds is internal, or because we
   * could not read that IPv4 at all.
   *
   * Where the IPv4 sits inside a NAT64 address depends on the prefix length (RFC 6052 section 2.2), and the suffix
   * a gateway ignores is free for the caller to fill in - so reading one position and trusting the rest is how
   * `64:ff9b:1:7f00:0:100:1:1` passes as `0.1.0.1` while a gateway connects to `127.0.0.1`. Only the one globally
   * routable form is read; everything else under this prefix is refused outright, because it is by definition a
   * translator inside somebody's deployment and its prefix length is not ours to guess.
   */
  private fun nat64MustBeRefused(bytes: ByteArray): Boolean {
    if ((4..11).all { bytes[it].toInt() == 0 }) return isInternalIpv4(bytes.copyOfRange(12, 16))
    return true
  }

  private fun isInternalIpv4(embedded: ByteArray): Boolean =
    try {
      isBlockedRange(InetAddress.getByAddress(embedded))
    } catch (_: Exception) {
      true
    }

  private fun isNat64Prefix(bytes: ByteArray): Boolean {
    val prefix = bytes.copyOfRange(0, 4).map { it.toInt() and 0xFF }
    return prefix == listOf(0x00, 0x64, 0xFF, 0x9B)
  }
}
