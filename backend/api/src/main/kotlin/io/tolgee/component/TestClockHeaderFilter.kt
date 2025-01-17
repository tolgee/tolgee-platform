package io.tolgee.component

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TestClockHeaderFilter(
  private val currentDateProvider: CurrentDateProvider,
) : OncePerRequestFilter() {
  companion object {
    const val TOLGEE_TEST_CLOCK_HEADER_NAME = "X-Tolgee-Test-Clock"
  }

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    response.addHeader(TOLGEE_TEST_CLOCK_HEADER_NAME, currentDateProvider.date.time.toString())
    filterChain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return currentDateProvider.forcedDate == null
  }
}
