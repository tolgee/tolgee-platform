package io.tolgee.component.reporting

import com.posthog.java.PostHog
import io.tolgee.activity.UtmDataHolder
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EventRecorder(
  private val postHog: PostHog?,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
  private val userAccountService: UserAccountService,
  private val utmDataHolder: UtmDataHolder,
) {

  @Lazy
  @Autowired
  private lateinit var selfProxied: EventRecorder

  @Async
  fun captureAsync(data: OnEventToCaptureEvent) {
    val filledData = fillData(data)
    captureWithPostHog(filledData)
  }


  @TransactionalEventListener
  fun capture(data: OnEventToCaptureEvent) {
    if (postHog == null) return
    selfProxied.captureAsync(data.copy(utmData = utmDataHolder.data))
  }

  private fun captureWithPostHog(data: OnEventToCaptureEvent) {
    val userAccountDto = data.userAccountDto ?: return
    postHog?.capture(
      userAccountDto.id.toString(), data.eventName,
      mapOf(
        "${'$'}set" to mapOf(
          "email" to userAccountDto.username,
          "name" to userAccountDto.name,
        ),
        "organizationId" to data.organizationId,
        "organizationName" to data.organizationName,
      ) + (data.utmData ?: emptyMap())
    )
  }

  private fun fillData(data: OnEventToCaptureEvent): OnEventToCaptureEvent {
    val projectDto = data.projectDto ?: data.projectId?.let { projectService.findDto(it) }
    val organizationId = data.organizationId ?: projectDto?.organizationOwnerId
    val organization = organizationId?.let { organizationService.get(it) }
    val userAccountDto = data.userAccountDto ?: data.userAccountId?.let { userAccountService.findDto(it) }
    return OnEventToCaptureEvent(
      eventName = data.eventName,
      projectDto = projectDto,
      projectId = data.projectId,
      organizationId = organizationId,
      organizationName = organization?.name,
      userAccountId = data.userAccountId,
      userAccountDto = userAccountDto
    )
  }
}
