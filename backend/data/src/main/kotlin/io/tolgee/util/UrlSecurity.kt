package io.tolgee.util

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URI

/**
 * Validates URLs to prevent Server-Side Request Forgery (SSRF) attacks.
 * Blocks requests to internal/private network addresses, loopback, and link-local ranges.
 */
object UrlSecurity {
  /**
   * Validates that the given URL is a safe external URL.
   * Throws [BadRequestException] if the URL is malformed, uses a non-HTTP(S) scheme,
   * or the host is a known private/internal address.
   */
  fun validateUrl(url: String) {
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

    val lowerHost = host.lowercase()
    if (lowerHost == "localhost" || lowerHost.endsWith(".localhost")) {
      throw BadRequestException(Message.URL_NOT_VALID)
    }

    // For IPv4 literals, InetAddress.getByName parses without DNS lookup
    val address =
      try {
        InetAddress.getByName(host).takeIf { it is Inet4Address && it.hostAddress == host }
      } catch (_: Exception) {
        null
      }

    if (address != null &&
      (
        address.isLoopbackAddress ||
          address.isSiteLocalAddress ||
          address.isLinkLocalAddress ||
          address.isAnyLocalAddress ||
          address.isMulticastAddress
      )
    ) {
      throw BadRequestException(Message.URL_NOT_VALID)
    }
  }
}
