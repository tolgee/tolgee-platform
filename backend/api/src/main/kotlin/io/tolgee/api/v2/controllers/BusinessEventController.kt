package io.tolgee.api.v2.controllers

import io.sentry.Sentry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.dtos.request.BusinessEventReportRequest
import io.tolgee.dtos.request.IdentifyRequest
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/public/business-events"])
@Tag(name = "Business events reporting")
@OpenApiHideFromPublicDocs
class BusinessEventController(
  private val businessEventPublisher: BusinessEventPublisher,
  private val securityService: SecurityService,
  private val organizationRoleService: OrganizationRoleService,
  private val projectService: ProjectService,
  private val authenticationFacade: AuthenticationFacade,
) : Logging {
  @PostMapping("/report")
  @Operation(summary = "Reports business event")
  fun report(
    @RequestBody eventData: BusinessEventReportRequest,
  ) {
    try {
      val projectId = eventData.projectId?.takeIf { securityService.hasAnyProjectPermission(it) }
      val owningOrganizationId = projectId?.let { owningOrganizationId(it) }
      val organizationId =
        eventData.organizationId
          ?.takeIf { canReportOrganization(it) }
          ?.takeIf { agreesWithReportedProject(it, owningOrganizationId) }
      val backedClaims = eventData.copy(projectId = projectId, organizationId = organizationId)
      logDroppedClaims(eventData, projectId, organizationId)
      businessEventPublisher.publish(backedClaims)
    } catch (e: Throwable) {
      logger.error("Error storing event", e)
      Sentry.captureException(e)
    }
  }

  @PostMapping("/identify")
  @Operation(summary = "Identifies user")
  fun identify(
    @RequestBody eventData: IdentifyRequest,
  ) {
    try {
      businessEventPublisher.publish(eventData)
    } catch (e: Throwable) {
      logger.error("Error storing event", e)
      Sentry.captureException(e)
    }
  }

  private fun owningOrganizationId(projectId: Long): Long? = projectService.findDto(projectId)?.organizationOwnerId

  private fun canReportOrganization(organizationId: Long): Boolean {
    val user = authenticationFacade.authenticatedUserOrNull ?: return false
    return organizationRoleService.canUserView(user, organizationId)
  }

  private fun agreesWithReportedProject(
    organizationId: Long,
    owningOrganizationId: Long?,
  ): Boolean = owningOrganizationId == null || organizationId == owningOrganizationId

  private fun logDroppedClaims(
    eventData: BusinessEventReportRequest,
    backedProjectId: Long?,
    backedOrganizationId: Long?,
  ) {
    if (backedProjectId == eventData.projectId && backedOrganizationId == eventData.organizationId) {
      return
    }
    logger.debug(
      "Dropped unbacked claim(s) from business event {}; claimed project {}, organization {}; " +
        "backed project {}, organization {}",
      eventData.eventName,
      eventData.projectId,
      eventData.organizationId,
      backedProjectId,
      backedOrganizationId,
    )
  }
}
