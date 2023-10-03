package io.tolgee.component

import io.tolgee.util.VersionProvider
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class VersionFilter(
  private val versionProvider: VersionProvider
) : OncePerRequestFilter() {
  companion object {
    const val TOLGEE_VERSION_HEADER_NAME = "X-Tolgee-Version"
  }

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    response.addHeader(TOLGEE_VERSION_HEADER_NAME, versionProvider.version)
    filterChain.doFilter(request, response)
  }
}
