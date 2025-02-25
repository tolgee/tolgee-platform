package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.notification.NotificationSettingModel
import io.tolgee.hateoas.notification.NotificationSettingsModelAssembler
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationSettingsService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/notifications-settings",
  ],
)
@Tag(name = "Notifications", description = "Manipulates notification settings")
class NotificationSettingsController(
  private val notificationSettingsService: NotificationSettingsService,
  private val authenticationFacade: AuthenticationFacade,
  private val notificationSettingsModelAssembler: NotificationSettingsModelAssembler,
) {
  @GetMapping
  @Operation(
    summary = "Get notification settings",
    description = "Returns notification settings of the currently logged in user",
  )
  @AllowApiAccess
  fun getNotificationsSettings(): NotificationSettingModel {
    val data = notificationSettingsService.getSettings(authenticationFacade.authenticatedUserEntity)
    return notificationSettingsModelAssembler.toModel(data)
  }

  @PutMapping
  @Operation(summary = "Save notification setting", description = "Saves new value for given parameters")
  @AllowApiAccess
  fun putNotificationSetting(
    group: NotificationTypeGroup,
    channel: NotificationChannel,
    enabled: Boolean,
  ) {
    if (group == NotificationTypeGroup.ACCOUNT_SECURITY) {
      throw BadRequestException("Account security settings cannot be changed.")
    }

    notificationSettingsService.save(authenticationFacade.authenticatedUserEntity, group, channel, enabled)
  }
}
