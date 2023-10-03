package io.tolgee.activity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.sentry.Sentry
import io.tolgee.component.reporting.SdkInfoProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.ScopeNotActiveException
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ActivityHandlerInterceptor(
  private val activityHolder: ActivityHolder,
  private val sdkInfoProvider: SdkInfoProvider
) : HandlerInterceptor {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (handler !is HandlerMethod) {
      return super.preHandle(request, response, handler)
    }

    try {
      val activityAnnotation = AnnotationUtils.getAnnotation(handler.method, RequestActivity::class.java)
      if (activityAnnotation != null) {
        activityHolder.activity = activityAnnotation.activity
      }

      assignUtmData(request)
      assignSdkInfo(request)
    } catch (e: ScopeNotActiveException) {
      logger.debug("Activity filter called outside of request scope")
    }

    return true
  }

  private fun assignSdkInfo(request: HttpServletRequest) {
    activityHolder.sdkInfo = sdkInfoProvider.getSdkInfo(request)
  }

  fun assignUtmData(request: HttpServletRequest) {
    try {
      val headerValue = request.getHeader(UTM_HEADER_NAME) ?: return
      val parsed = parseUtmValues(headerValue) ?: return
      activityHolder.utmData = parsed
    } catch (e: Exception) {
      Sentry.captureException(e)
      logger.error("Exception occurred while assigning UTM data", e)
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
