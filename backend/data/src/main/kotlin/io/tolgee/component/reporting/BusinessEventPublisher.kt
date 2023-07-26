package io.tolgee.component.reporting

import io.sentry.Sentry
import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.UtmData
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Caches
import io.tolgee.dtos.request.BusinessEventReportRequest
import io.tolgee.dtos.request.IdentifyRequest
import io.tolgee.security.AuthenticationFacade
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class BusinessEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val applicationContext: ApplicationContext,
  private val authenticationFacade: AuthenticationFacade,
  private val cacheManager: CacheManager,
  private val currentDateProvider: CurrentDateProvider,
  private val sdkInfoProvider: SdkInfoProvider
) : Logging {

  fun publish(request: BusinessEventReportRequest) {
    publish(
      OnBusinessEventToCaptureEvent(
        eventName = request.eventName,
        projectId = request.projectId,
        organizationId = request.organizationId,
        utmData = getUtmData(),
        data = request.data,
        anonymousUserId = request.anonymousUserId
      )
    )
  }

  fun publish(event: OnBusinessEventToCaptureEvent) {
    applicationEventPublisher.publishEvent(
      event.copy(
        utmData = event.utmData ?: getUtmData(),
        userAccountId = event.userAccountId ?: event.userAccountDto?.id ?: authenticationFacade.userAccountOrNull?.id,
        userAccountDto = event.userAccountDto ?: authenticationFacade.userAccountOrNull,
        data = getDataWithSdkInfo(event.data)
      )
    )
  }

  fun publish(event: IdentifyRequest) {
    authenticationFacade.userAccountOrNull?.id?.let { userId ->
      applicationEventPublisher.publishEvent(
        OnIdentifyEvent(
          userAccountId = userId,
          anonymousUserId = event.anonymousUserId
        )
      )
    }
  }

  fun publishOnceInTime(
    event: OnBusinessEventToCaptureEvent,
    onceIn: Duration,
    keyProvider: (e: OnBusinessEventToCaptureEvent) -> String = { it.eventName + "_" + it.userAccountId }
  ) {
    val key = keyProvider(event)
    if (shouldPublishOnceInTime(key, onceIn)) {
      publish(event)
      cachePublished(key)
    }
  }

  private fun cachePublished(key: String) {
    getEventThrottlingCache()?.put(key, ThrottledEventInCache(currentDateProvider.date.time))
  }

  private fun shouldPublishOnceInTime(
    key: String,
    onceIn: Duration
  ): Boolean {
    val cache = getEventThrottlingCache()
    val cached =
      cache?.get(key)?.get()
        as? ThrottledEventInCache ?: return true

    if (cached.publishedAt < currentDateProvider.date.time - onceIn.toMillis()) {
      cache.evict(key)
      return true
    }

    return false
  }

  private fun getEventThrottlingCache(): Cache? = cacheManager.getCache(Caches.BUSINESS_EVENT_THROTTLING)

  fun getUtmData(): UtmData {
    return try {
      applicationContext.getBean(ActivityHolder::class.java).utmData
    } catch (e: Throwable) {
      logger.error("Could not get utm data from activity holder", e)
      Sentry.captureException(e)
      null
    }
  }

  fun getDataWithSdkInfo(data: Map<String, Any?>?): Map<String, Any?>? {
    val sdkInfoMap = sdkInfoProvider.getSdkInfo() ?: return data
    return getDataWithSdkInfo(sdkInfoMap, data)
  }

  fun getDataWithSdkInfo(
    sdkInfoMap: Map<String, String?>,
    data: Map<String, Any?>?
  ): Map<String, Any?>? {
    if (sdkInfoMap.values.all { it == null }) {
      return data
    }

    return data?.plus(sdkInfoMap) ?: sdkInfoMap
  }
}
