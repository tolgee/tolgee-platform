package io.tolgee.util

import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class RequestIpProvider {
  /**
   * The address the connection actually came from, which a client cannot choose - unlike the
   * forwarding headers [getClientIp] reads. Anything that ends up in the audit trail or in front of
   * a user as evidence uses this, the same value the rate limiter buckets on.
   *
   * Behind a proxy this is the proxy's own address, which is useless for locating anyone but is at
   * least never a lie. `server.forward-headers-strategy` would make the container derive it from
   * `X-Forwarded-For` instead - do not set it unless the proxy in front is known to overwrite that
   * header rather than append to it, because otherwise it hands the client back control of this
   * value and of the rate limiter's bucket key.
   */
  fun getTrustedClientIp(): String? {
    if (RequestContextHolder.getRequestAttributes() == null) {
      return null
    }
    val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
    return request.remoteAddr?.take(MAX_IP_LENGTH)
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
