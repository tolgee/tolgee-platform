package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.notification.NotificationSettingModel
import io.tolgee.hateoas.notification.NotificationSettingTypeGroup
import io.tolgee.security.authentication.AllowApiAccess
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/notifications-settings",
  ],
)
@Tag(name = "Notifications settings", description = "Manipulates notification settings")
class NotificationSettingsController {
  @GetMapping
  @Operation(summary = "Gets notifications settings of the currently logged in user.")
  @AllowApiAccess
  fun getNotificationsSettings(): Map<NotificationSettingTypeGroup, NotificationSettingModel> =
    mapOf(
      NotificationSettingTypeGroup.ACCOUNT_SECURITY to NotificationSettingModel(true, true),
      NotificationSettingTypeGroup.TASKS to NotificationSettingModel(false, false),
    )
}
