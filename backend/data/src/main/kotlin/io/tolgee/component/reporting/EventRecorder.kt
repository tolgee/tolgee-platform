package io.tolgee.component.reporting

import com.posthog.java.PostHog
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(PostHog::class)
class EventRecorder(
  private val postHog: PostHog,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
  private val userAccountService: UserAccountService,
) {
  @EventListener
  fun capture(data: OnEventToCaptureEvent) {
    val filledData = fillData(data)
    captureWithPostHog(filledData)
  }

  private fun captureWithPostHog(data: OnEventToCaptureEvent) {
    val userAccountDto = data.userAccountDto ?: return
    postHog.capture(
      userAccountDto.id.toString(), data.eventName,
      mapOf(
        "organizationId" to data.organizationId,
        "organizationName" to data.organizationName,
        "${'$'}set" to mapOf(
          "email" to data.userAccountId,
          "name" to userAccountDto.name,
        )
      )
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
