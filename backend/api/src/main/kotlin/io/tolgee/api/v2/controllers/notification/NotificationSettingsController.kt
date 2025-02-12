package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.notification.NotificationSettingModel
import io.tolgee.hateoas.notification.NotificationSettingsModelAssembler
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationSettingService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/notifications-settings",
  ],
)
@Tag(name = "Notifications settings", description = "Manipulates notification settings")
class NotificationSettingsController(
  private val notificationSettingService: NotificationSettingService,
  private val authenticationFacade: AuthenticationFacade,
  private val notificationSettingsModelAssembler: NotificationSettingsModelAssembler,
) {
  @GetMapping
  @Operation(summary = "Gets notifications settings of the currently logged in user.")
  @AllowApiAccess
  fun getNotificationsSettings(): NotificationSettingModel {
    Thread.sleep(1000)
    val data = notificationSettingService.getSettings(authenticationFacade.authenticatedUserEntity)
    return notificationSettingsModelAssembler.toModel(data)
  }

  @PutMapping
  @Operation(summary = "Saves a new value of setting.")
  @AllowApiAccess
  fun putNotificationSetting(
    group: NotificationTypeGroup,
    channel: NotificationChannel,
    enabled: Boolean,
  ) {
    notificationSettingService.save(authenticationFacade.authenticatedUserEntity, group, channel, enabled)
  }
}
