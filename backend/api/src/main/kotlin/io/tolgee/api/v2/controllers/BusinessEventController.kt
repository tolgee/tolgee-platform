package io.tolgee.api.v2.controllers

import io.sentry.Sentry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.dtos.request.BusinessEventReportRequest
import io.tolgee.dtos.request.IdentifyRequest
import io.tolgee.service.organization.OrganizationRoleService
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
class BusinessEventController(
  private val businessEventPublisher: BusinessEventPublisher,
  private val securityService: SecurityService,
  private val organizationRoleService: OrganizationRoleService,
) : Logging {
  @PostMapping("/report")
  @Operation(summary = "Reports business event")
  fun report(@RequestBody eventData: BusinessEventReportRequest) {
    try {
      eventData.projectId?.let { securityService.checkAnyProjectPermission(it) }
      eventData.organizationId?.let { organizationRoleService.checkUserCanView(it) }
      businessEventPublisher.publish(eventData)
    } catch (e: Throwable) {
      logger.error("Error storing event", e)
      Sentry.captureException(e)
    }
  }

  @PostMapping("/identify")
  @Operation(summary = "Identifies user")
  fun identify(@RequestBody eventData: IdentifyRequest) {
    try {
      businessEventPublisher.publish(eventData)
    } catch (e: Throwable) {
      logger.error("Error storing event", e)
      Sentry.captureException(e)
    }
  }
}
