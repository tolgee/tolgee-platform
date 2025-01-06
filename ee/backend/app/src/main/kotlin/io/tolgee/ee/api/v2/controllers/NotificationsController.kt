package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.NotificationModel
import io.tolgee.service.NotificationService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/notifications",
  ],
)
@Tag(name = "Notifications", description = "Manipulates notifications")
class NotificationsController(
  private val notificationService: NotificationService,
  private val authenticationFacade: AuthenticationFacade,
) {

  @GetMapping
  @Operation(summary = "Get notifications")
  @AllowApiAccess
  fun getNotifications(): NotificationsResponse {
    return NotificationsResponse(
      notificationService.getNotifications(authenticationFacade.authenticatedUser.id)
    )
  }
}

data class NotificationsResponse(
  val notifications: List<NotificationModel>,
)
