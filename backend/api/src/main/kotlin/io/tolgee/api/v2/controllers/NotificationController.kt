package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.notification.NotificationResponse
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationService
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
class NotificationController(
  private val notificationService: NotificationService,
  private val authenticationFacade: AuthenticationFacade,
) {

  @GetMapping
  @Operation(summary = "Gets notifications of the currently logged in user, newest is first.")
  @AllowApiAccess
  fun getNotifications(): NotificationResponse {
    return NotificationResponse(
      notificationService.getNotifications(authenticationFacade.authenticatedUser.id)
    )
  }
}
