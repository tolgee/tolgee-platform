package io.tolgee.component.reporting

import io.tolgee.configuration.tolgee.PlausibleProperties
import io.tolgee.util.RequestIpProvider
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

private val ALLOWED_EVENTS =
  arrayOf(
    "SIGN_UP",
    "DEMO_PROJECT_VIEWED",
    "BILLING_CLOUD_SUBSCRIPTIONS_VIEW",
    "UI_CLOUD_SUBSCRIPTION_UPDATE_SUCCESS",
    "UI_SELF_HOSTED_SUBSCRIPTION_CREATED",
  )

@Component
class PlausibleBusinessEventReporter(
  private val plausibleProperties: PlausibleProperties,
  private val restTemplate: RestTemplate,
  private val requestIpProvider: RequestIpProvider,
) {
  @Lazy
  @Autowired
  private lateinit var selfProxied: PlausibleBusinessEventReporter

  @EventListener
  fun capture(data: OnBusinessEventToCaptureEvent) {
    if (plausibleProperties.domain == null) return
    if (data.eventName !in ALLOWED_EVENTS) return
    val userInfo = getUserInfo() ?: return
    selfProxied.captureAsync(data, userInfo)
  }

  private fun getUserInfo(): RequestInfo? {
    return RequestInfo(
      ip = getIp() ?: return null,
      userAgent = getUserAgent() ?: return null,
      url = getUrl() ?: return null,
    )
  }

  @Async
  fun captureAsync(
    data: OnBusinessEventToCaptureEvent,
    requestInfo: RequestInfo,
  ) {
    val headers =
      HttpHeaders().apply {
        set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        set("X-Forwarded-For", requestInfo.ip)
        set("User-Agent", requestInfo.userAgent)
      }

    val event = getEvent(data, requestInfo.url)
    val requestEntity = HttpEntity(event, headers)

    val response = restTemplate.exchange<String>(endpoint, HttpMethod.POST, requestEntity)

    if (!response.statusCode.is2xxSuccessful) {
      throw RuntimeException(
        "Failed to send event to plausible. " +
          "Server returned " +
          "${response.statusCode}\n\n${response.body}",
      )
    }
  }

  private fun getEvent(
    data: OnBusinessEventToCaptureEvent,
    url: String,
  ): PlausibleEvent {
    return PlausibleEvent(
      name = data.eventName,
      url = url,
      domain = plausibleProperties.domain!!,
      props =
        data.data
          ?.mapNotNull {
            it.key to it.value.toString()
          }?.toMap(),
    )
  }

  private fun getUrl(): String? {
    return getRequest()?.requestURL?.toString()
  }

  private fun getIp(): String? {
    return requestIpProvider.getClientIp()
  }

  private fun getRequest(): HttpServletRequest? {
    return (RequestContextHolder.currentRequestAttributes() as? ServletRequestAttributes)?.request
  }

  private fun getUserAgent(): String? {
    return getRequest()?.getHeader("User-Agent")
  }

  private val endpoint by lazy {
    plausibleProperties.url + "/api/event"
  }

  data class PlausibleEvent(
    val name: String,
    val url: String,
    val domain: String,
    val props: Map<String, String>? = null,
  )

  data class RequestInfo(
    val ip: String,
    val userAgent: String,
    val url: String,
  )
}
