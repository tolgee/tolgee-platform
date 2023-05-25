package io.tolgee.component

import io.tolgee.util.VersionProvider
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class VersionFilter() : OncePerRequestFilter() {
  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    response.addHeader("X-Tolgee-Version", VersionProvider.version)
    filterChain.doFilter(request, response)
  }
}
