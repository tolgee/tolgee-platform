package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.notification.NotificationDigestFrequencyRequest
import io.tolgee.dtos.request.notification.NotificationSettingsRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.notification.NotificationSettingModel
import io.tolgee.hateoas.notification.NotificationSettingsModelAssembler
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationDigestStateService
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
  private val notificationDigestStateService: NotificationDigestStateService,
) {
  @GetMapping
  @Operation(
    summary = "Get notification settings",
    description = "Returns notification settings of the currently logged in user",
  )
  @AllowApiAccess
  fun getNotificationsSettings(): NotificationSettingModel {
    val user = authenticationFacade.authenticatedUserEntity
    val data = notificationSettingsService.getSettings(user)
    return notificationSettingsModelAssembler.toModel(data, notificationDigestStateService.getFrequency(user.id))
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
    if (request.type != null && request.group != NotificationTypeGroup.LOCALIZATION) {
      throw BadRequestException("Type can only be set for the localization group.")
    }
    val user = authenticationFacade.authenticatedUserEntity
    if (request.group == NotificationTypeGroup.LOCALIZATION) {
      val type = request.type
      if (type == null || type.group != NotificationTypeGroup.LOCALIZATION) {
        throw BadRequestException("A localization notification type is required.")
      }
      notificationSettingsService.saveForType(user, type, request.channel, request.enabled)
      return
    }
    notificationSettingsService.save(user, request.group, request.channel, request.enabled)
  }

  @PutMapping("/digest-frequency")
  @Operation(summary = "Set digest frequency", description = "Sets how often the notification digest email is sent")
  @AllowApiAccess
  fun putDigestFrequency(
    @RequestBody @Valid request: NotificationDigestFrequencyRequest,
  ) {
    notificationDigestStateService.setFrequency(authenticationFacade.authenticatedUser.id, request.frequency)
  }
}
