package io.tolgee.util

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
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
   * or resolves to a private/internal IP address.
   */
  fun validateUrl(url: String) {
    val uri =
      try {
        URI(url)
      } catch (e: Exception) {
        throw BadRequestException(Message.URL_NOT_VALID)
      }

    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") {
      throw BadRequestException(Message.URL_NOT_VALID)
    }

    val host = uri.host ?: throw BadRequestException(Message.URL_NOT_VALID)

    // Resolve DNS to check the actual IP address (prevents DNS rebinding with private IPs)
    val addresses =
      try {
        InetAddress.getAllByName(host)
      } catch (e: Exception) {
        throw BadRequestException(Message.URL_NOT_VALID)
      }

    for (address in addresses) {
      if (isPrivateOrReserved(address)) {
        throw BadRequestException(Message.URL_NOT_VALID)
      }
    }
  }

  private fun isPrivateOrReserved(address: InetAddress): Boolean {
    return address.isLoopbackAddress ||
      address.isSiteLocalAddress ||
      address.isLinkLocalAddress ||
      address.isAnyLocalAddress ||
      address.isMulticastAddress
  }
}
