package io.tolgee.component.reporting

import com.posthog.java.PostHog
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class BusinessEventReporter(
  private val postHog: PostHog?,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
  private val userAccountService: UserAccountService,
) {

  @Lazy
  @Autowired
  private lateinit var selfProxied: BusinessEventReporter

  @Async
  fun captureAsync(data: OnBusinessEventToCaptureEvent) {
    val filledData = fillOtherData(data)
    captureWithPostHog(filledData)
  }

  @EventListener
  fun capture(data: OnBusinessEventToCaptureEvent) {
    if (postHog == null) return
    selfProxied.captureAsync(data)
  }

  @EventListener
  fun identify(data: OnIdentifyEvent) {
    val dto = userAccountService.findDto(data.userAccountId) ?: return
    postHog?.capture(
      data.userAccountId.toString(),
      "${'$'}identify",
      mapOf(
        "${'$'}anon_distinct_id" to data.anonymousUserId,
      ) + getSetMapOfUserData(dto)
    )
  }

  private fun captureWithPostHog(data: OnBusinessEventToCaptureEvent) {
    val id = data.userAccountDto?.id ?: data.instanceId ?: data.anonymousUserId
    val setEntry = getIdentificationMapForPostHog(data)

    postHog?.capture(
      id.toString(), data.eventName,
      mapOf(
        "organizationId" to data.organizationId,
        "organizationName" to data.organizationName,
      ) + (data.utmData ?: emptyMap()) + (data.data ?: emptyMap()) + setEntry
    )
  }

  /**
   * PostHog accepts user information in $set property.
   *
   * This method returns map with $set property if user information is present
   * or if instanceId is sent by self-hosted instance.
   */
  private fun getIdentificationMapForPostHog(data: OnBusinessEventToCaptureEvent): Map<String, Any?> {
    val setEntry = data.userAccountDto?.let { userAccountDto ->
      getSetMapOfUserData(userAccountDto)
    } ?: data.instanceId?.let {
      mapOf(
        "${'$'}set" to mapOf(
          "instanceId" to it
        )
      )
    } ?: emptyMap()
    return setEntry + getAnonIdMap(data)
  }

  private fun getSetMapOfUserData(userAccountDto: UserAccountDto) = mapOf(
    "${'$'}set" to mapOf(
      "email" to userAccountDto.username,
      "name" to userAccountDto.name,
    ),
  )

  fun getAnonIdMap(data: OnBusinessEventToCaptureEvent): Map<String, String> {
    return (
      data.anonymousUserId?.let {
        mapOf(
          "${'$'}anon_distinct_id" to data.anonymousUserId,
        )
      }
      ) ?: emptyMap()
  }

  private fun fillOtherData(data: OnBusinessEventToCaptureEvent): OnBusinessEventToCaptureEvent {
    val projectDto = data.projectDto ?: data.projectId?.let { projectService.findDto(it) }
    val organizationId = data.organizationId ?: projectDto?.organizationOwnerId
    val organizationName = data.organizationName ?: organizationId?.let { organizationService.get(it).name }
    val userAccountDto = data.userAccountDto ?: data.userAccountId?.let { userAccountService.findDto(it) }
    return data.copy(
      projectDto = projectDto,
      organizationId = organizationId,
      organizationName = organizationName,
      userAccountDto = userAccountDto
    )
  }
}
