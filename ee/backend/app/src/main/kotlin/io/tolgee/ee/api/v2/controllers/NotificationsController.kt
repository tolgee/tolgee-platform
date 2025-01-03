package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.security.authentication.AllowApiAccess
import org.springframework.http.ResponseEntity
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
class NotificationsController {

  @GetMapping
  @Operation(summary = "Get notifications")
  @AllowApiAccess
  fun getNotifications(): ResponseEntity<NotificationsResponse> {
    return ResponseEntity.ok(
      NotificationsResponse(
        listOf(
          Notification(1, 1000000001, "Task One"),
          Notification(2, 1000000002, "Task Two"),
          Notification(3, 1000000003, "Task Three"),
          Notification(4, 1000000004, "Task Four"),
        )
      )
    )
  }
}

data class NotificationsResponse(
  val notifications: List<Notification>,
)

data class Notification(
  val id: Long,
  val linkedEntityId: Long,
  val linkedEntityName: String,
)
