package io.tolgee.util

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class RequestIpProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  /**
   * The address to record as evidence - what the session list shows and the audit log keeps.
   *
   * A proxy appends the address it actually observed to `X-Forwarded-For`, while a client can only
   * prepend entries of its own, so counting from the end of the list is what makes the value
   * unforgeable. With no proxy configured the connection's own address is used, which is the same
   * value the rate limiter buckets on. Never the front of the list: that entry is whatever the
   * caller chose to send.
   */
  fun getTrustedClientIp(): String? {
    val request = currentRequest() ?: return null
    val hops = tolgeeProperties.authentication.sessionAudit.trustedProxyCount
    if (hops <= 0) return request.remoteAddr?.take(MAX_IP_LENGTH)

    val forwarded =
      request
        .getHeader("X-Forwarded-For")
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: return request.remoteAddr?.take(MAX_IP_LENGTH)

    return forwarded
      .getOrNull(forwarded.size - hops)
      ?.take(MAX_IP_LENGTH)
      ?: request.remoteAddr?.take(MAX_IP_LENGTH)
  }

  private fun currentRequest(): jakarta.servlet.http.HttpServletRequest? {
    val attributes = RequestContextHolder.getRequestAttributes() ?: return null
    return (attributes as ServletRequestAttributes).request
  }

  fun getClientIp(): String? {
    if (RequestContextHolder.getRequestAttributes() == null) {
      return null
    }

    val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
    for (header in IP_HEADER_CANDIDATES) {
      val ipList = request.getHeader(header)
      if (ipList != null && ipList.isNotEmpty() && !"unknown".equals(ipList, ignoreCase = true)) {
        val ip = ipList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.firstOrNull()
        return ip?.take(MAX_IP_LENGTH)
      }
    }

    return request.remoteAddr?.take(MAX_IP_LENGTH)
  }

  companion object {
    /**
     * Forwarding headers are attacker-controlled, while the longest genuine IPv6 literal is 45
     * characters. Truncating here keeps over-long values from breaking every consumer's column.
     */
    const val MAX_IP_LENGTH = 64

    private val IP_HEADER_CANDIDATES =
      arrayOf(
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR",
      )
  }
}
