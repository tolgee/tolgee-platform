package io.tolgee.component.reporting

import io.tolgee.activity.UtmDataHolder
import io.tolgee.component.PostHogWrapper
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EventRecorder(
  private val postHogWrapper: PostHogWrapper,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
  private val userAccountService: UserAccountService,
  private val utmDataHolder: UtmDataHolder
) {
  @TransactionalEventListener
  @Async
  fun capture(data: OnEventToCaptureEvent) {
    postHogWrapper.postHog ?: return
    val filledData = fillData(data)
    captureWithPostHog(filledData)
  }

  private fun captureWithPostHog(data: OnEventToCaptureEvent) {
    val userAccountDto = data.userAccountDto ?: return
    postHogWrapper.postHog?.capture(
      userAccountDto.id.toString(), data.eventName,
      mapOf(
        "${'$'}set" to mapOf(
          "email" to userAccountDto.username,
          "name" to userAccountDto.name,
        ),
        "organizationId" to data.organizationId,
        "organizationName" to data.organizationName,
      ) + (utmDataHolder.data ?: emptyMap())
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
