package io.tolgee.component.reporting

import com.posthog.server.PostHog
import io.tolgee.component.reporting.PostHogGroupIdentifier.Companion.GROUP_TYPE
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.filterValueNotNull
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class PostHogBusinessEventReporter(
  private val postHog: PostHog?,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
  private val userAccountService: UserAccountService,
  private val entityManager: EntityManager,
  private var postHogGroupIdentifier: PostHogGroupIdentifier?,
) {
  @Lazy
  @Autowired
  private lateinit var selfProxied: PostHogBusinessEventReporter

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
      ) + getSetMapOfUserData(dto),
    )
  }

  private fun captureWithPostHog(data: OnBusinessEventToCaptureEvent) {
    val id = data.userAccountDto?.id ?: data.instanceId ?: data.anonymousUserId
    val setEntry = getIdentificationMapForPostHog(data)

    val map =
      mapOf(
        "${'$'}groups" to
          mapOf(
            "project" to data.projectDto?.id,
            GROUP_TYPE to data.organizationId,
          ),
        "organizationId" to data.organizationId,
        "organizationName" to data.organizationName,
      ) + (data.utmData ?: emptyMap()) + (data.data ?: emptyMap()) + setEntry

    postHog?.capture(
      id.toString(),
      data.eventName,
      map.filterValueNotNull(),
    )

    postHogGroupIdentifier?.identifyOrganization(organizationId = data.organizationId ?: return)
  }

  /**
   * PostHog accepts user information in $set property.
   *
   * This method returns map with $set property if user information is present
   * or if instanceId is sent by self-hosted instance.
   */
  private fun getIdentificationMapForPostHog(data: OnBusinessEventToCaptureEvent): Map<String, Any?> {
    val setEntry =
      data.userAccountDto?.let { userAccountDto ->
        getSetMapOfUserData(userAccountDto)
      } ?: data.instanceId?.let {
        mapOf(
          "${'$'}set" to
            mapOf(
              "instanceId" to it,
            ),
        )
      } ?: emptyMap()
    return setEntry + getAnonIdMap(data)
  }

  private fun getSetMapOfUserData(userAccountDto: UserAccountDto) =
    mapOf(
      "${'$'}set" to
        mapOf(
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
    val userAccountId = data.userAccountId ?: findOwnerUserByOrganizationId(organizationId)
    val userAccountDto = data.userAccountDto ?: userAccountId?.let { userAccountService.findDto(it) }
    return data.copy(
      projectDto = projectDto,
      organizationId = organizationId,
      organizationName = organizationName,
      userAccountDto = userAccountDto,
    )
  }

  private fun findOwnerUserByOrganizationId(organizationId: Long?): Long? {
    organizationId ?: return null
    return entityManager
      .createQuery(
        """
      select u.id from UserAccount u 
      join u.organizationRoles orl on orl.organization.id = :organizationId
      where orl.type = io.tolgee.model.enums.OrganizationRoleType.OWNER
      order by u.id
      limit 1
    """,
        Long::class.java,
      ).setParameter("organizationId", organizationId)
      .resultList
      .firstOrNull()
  }
}
