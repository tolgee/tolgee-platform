package io.tolgee.component

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.activity.UtmDataHolder
import io.tolgee.util.Logging
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class UtmReportingFilter(
  private val utmDataHolder: UtmDataHolder
) : OncePerRequestFilter(), Logging {
  companion object {
    const val UTM_HEADER_NAME = "X-Tolgee-Utm"
  }

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    assignHolder(request)
    filterChain.doFilter(request, response)
  }

  fun assignHolder(request: HttpServletRequest) {
    try {
      val headerValue = request.getHeader(UTM_HEADER_NAME) ?: return
      val parsed = parseUtmValues(headerValue) ?: return
      utmDataHolder.data = parsed
    } catch (e: Exception) {
      Sentry.captureException(e)
      logger.error(e)
    }
  }

  fun parseUtmValues(headerValue: String?): Map<String, Any?>? {
    val base64Decoded = Base64.getDecoder().decode(headerValue)
    val utmParamsJson = String(base64Decoded, StandardCharsets.UTF_8)
    val utmParams = mutableMapOf<String, String>()
    return jacksonObjectMapper().readValue(utmParamsJson, utmParams::class.java)
      .filterKeys { it.startsWith("utm_") }
  }
}
