package io.tolgee.activity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.component.reporting.SdkInfoProvider
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ActivityFilter(
  private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
  private val activityHolder: ActivityHolder,
  private val sdkInfoProvider: SdkInfoProvider
) : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {

    val activityAnnotation = getActivityAnnotation(request)

    if (activityAnnotation != null) {
      activityHolder.activity = activityAnnotation.activity
    }

    assignUtmData(request)
    assignSdkInfo(request)

    filterChain.doFilter(request, response)
  }

  private fun assignSdkInfo(request: HttpServletRequest) {
    activityHolder.sdkInfo = sdkInfoProvider.getSdkInfo(request)
  }

  private fun getActivityAnnotation(request: HttpServletRequest): RequestActivity? {
    val handlerMethod = (requestMappingHandlerMapping.getHandler(request)?.handler as HandlerMethod?)
    return handlerMethod?.getMethodAnnotation(RequestActivity::class.java)
  }

  fun assignUtmData(request: HttpServletRequest) {
    try {
      val headerValue = request.getHeader(UTM_HEADER_NAME) ?: return
      val parsed = parseUtmValues(headerValue) ?: return
      activityHolder.utmData = parsed
    } catch (e: Exception) {
      Sentry.captureException(e)
      logger.error(e)
    }
  }

  fun parseUtmValues(headerValue: String?): Map<String, Any?>? {
    val urlDecoded = URLDecoder.decode(headerValue, StandardCharsets.UTF_8)
    val base64Decoded = Base64.getDecoder().decode(urlDecoded)
    val utmParamsJson = String(base64Decoded, StandardCharsets.UTF_8)
    val utmParams = mutableMapOf<String, String>()
    return jacksonObjectMapper().readValue(utmParamsJson, utmParams::class.java)
      .filterKeys { it.startsWith("utm_") }
  }

  companion object {
    const val UTM_HEADER_NAME = "X-Tolgee-Utm"
  }
}
