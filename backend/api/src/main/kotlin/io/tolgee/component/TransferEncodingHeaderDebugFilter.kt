package io.tolgee.component

import io.tolgee.util.Logging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class TransferEncodingHeaderDebugFilter :
  OncePerRequestFilter(),
  Logging {
  init {
    logger.debug("TransferEncodingHeaderDebugFilter init")
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return !this.logger.isDebugEnabled
  }

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    filterChain.doFilter(request, response)
    val value = response.getHeader("Transport-Encoding")
    if (value != null) {
      logger.debug("Transport-Encoding: $value")
    }
  }
}
