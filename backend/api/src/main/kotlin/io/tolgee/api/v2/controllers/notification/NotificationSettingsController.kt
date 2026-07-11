package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.notification.NotificationSettingsRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.notification.NotificationSettingModel
import io.tolgee.hateoas.notification.NotificationSettingsModelAssembler
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationSettingsService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/notification-settings",
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
    @RequestBody @Valid request: NotificationSettingsRequest,
  ) {
    if (request.group == NotificationTypeGroup.ACCOUNT_SECURITY) {
      throw BadRequestException("Account security settings cannot be changed.")
    }

    notificationSettingsService.save(
      authenticationFacade.authenticatedUserEntity,
      request.group,
      request.channel,
      request.enabled,
    )
  }
}
