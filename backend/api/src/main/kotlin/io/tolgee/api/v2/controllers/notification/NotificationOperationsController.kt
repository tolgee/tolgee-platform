package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.notification.NotificationsMarkSeenRequest
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/",
  ],
)
@Tag(name = "Notifications", description = "Manipulates notifications")
class NotificationOperationsController(
  private val notificationService: NotificationService,
  private val authenticationFacade: AuthenticationFacade,
) {
  @PutMapping("/notifications-mark-seen")
  @Operation(summary = "Marks notifications of the currently logged in user with given IDs as seen.")
  @AllowApiAccess
  fun markNotificationsAsSeen(
    @RequestBody @Valid request: NotificationsMarkSeenRequest,
  ) {
    notificationService.markNotificationsAsSeen(request.notificationIds, authenticationFacade.authenticatedUser.id)
  }
}
