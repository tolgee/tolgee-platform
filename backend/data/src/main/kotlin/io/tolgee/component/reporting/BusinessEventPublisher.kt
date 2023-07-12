package io.tolgee.component.reporting

import io.sentry.Sentry
import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.UtmData
import io.tolgee.dtos.request.BusinessEventReportRequest
import io.tolgee.security.AuthenticationFacade
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class BusinessEventPublisher(
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val applicationContext: ApplicationContext,
  private val authenticationFacade: AuthenticationFacade,
) : Logging {
  fun publish(event: OnBusinessEventToCaptureEvent) {
    applicationEventPublisher.publishEvent(
      event.copy(
        utmData = event.utmData ?: getUtmData(),
        userAccountId = authenticationFacade.userAccountOrNull?.id,
        userAccountDto = authenticationFacade.userAccountOrNull
      )
    )
  }

  fun getUtmData(): UtmData {
    return try {
      applicationContext.getBean(ActivityHolder::class.java).utmData
    } catch (e: Throwable) {
      logger.error("Could not get utm data from activity holder", e)
      Sentry.captureException(e)
      null
    }
  }

  fun publish(request: BusinessEventReportRequest) {
    publish(
      OnBusinessEventToCaptureEvent(
        eventName = request.eventName,
        projectId = request.projectId,
        organizationId = request.organizationId,
        utmData = getUtmData(),
        data = request.data
      )
    )
  }
}
