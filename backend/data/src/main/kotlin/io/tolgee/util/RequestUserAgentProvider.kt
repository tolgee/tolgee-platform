package io.tolgee.util

import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class RequestUserAgentProvider {
  fun getUserAgent(): String? {
    val attributes = RequestContextHolder.getRequestAttributes() ?: return null
    val request = (attributes as ServletRequestAttributes).request
    return request.getHeader("User-Agent")?.take(MAX_USER_AGENT_LENGTH)
  }

  companion object {
    const val MAX_USER_AGENT_LENGTH = 255
  }
}
