package io.tolgee.api.v2.controllers.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.notification.NotificationDigestFrequencyRequest
import io.tolgee.dtos.request.notification.NotificationLanguagesRequest
import io.tolgee.dtos.request.notification.NotificationSettingsRequest
import io.tolgee.dtos.request.notification.ProjectMentionSettingRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.notification.NotificationLanguageModel
import io.tolgee.hateoas.notification.NotificationSettingModel
import io.tolgee.hateoas.notification.NotificationSettingsModelAssembler
import io.tolgee.hateoas.notification.ProjectMentionSettingModel
import io.tolgee.hateoas.notification.ProjectNotificationLanguagesModel
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.notification.NotificationDigestStateService
import io.tolgee.service.notification.NotificationLanguageSettingsService
import io.tolgee.service.notification.NotificationMentionMuteService
import io.tolgee.service.notification.NotificationSettingsService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
  private val notificationLanguageSettingsService: NotificationLanguageSettingsService,
  private val notificationMentionMuteService: NotificationMentionMuteService,
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

  @GetMapping("/languages")
  @Operation(
    summary = "Get notification languages",
    description = "Returns, per project, the languages the user can follow for work notifications",
  )
  @AllowApiAccess
  fun getNotificationLanguages(): List<ProjectNotificationLanguagesModel> =
    notificationLanguageSettingsService.getForUser(authenticationFacade.authenticatedUserEntity).map { project ->
      ProjectNotificationLanguagesModel(
        projectId = project.projectId,
        projectName = project.projectName,
        availableLanguages =
          project.availableLanguages.map { NotificationLanguageModel(it.id, it.tag, it.name, it.flagEmoji) },
        followedLanguageIds = project.followedLanguageIds,
        state = project.state,
      )
    }

  @PutMapping("/languages/{projectId}")
  @Operation(
    summary = "Set notification languages",
    description = "Sets the followed languages of the project. Empty mutes it, null goes back to the default.",
  )
  @AllowApiAccess
  fun putNotificationLanguages(
    @PathVariable projectId: Long,
    @RequestBody @Valid request: NotificationLanguagesRequest,
  ) {
    notificationLanguageSettingsService.set(authenticationFacade.authenticatedUser.id, projectId, request.languageIds)
  }

  @GetMapping("/mentions")
  @Operation(
    summary = "Get mention settings per project",
    description =
      "Returns the projects where the user commented or was mentioned, and whether their mentions are muted",
  )
  @AllowApiAccess
  fun getMentionSettings(): List<ProjectMentionSettingModel> =
    notificationMentionMuteService.getForUser(authenticationFacade.authenticatedUser.id).map {
      ProjectMentionSettingModel(projectId = it.projectId, projectName = it.projectName, muted = it.muted)
    }

  @PutMapping("/mentions/{projectId}")
  @Operation(summary = "Mute or unmute mentions from a project")
  @AllowApiAccess
  fun putMentionSetting(
    @PathVariable projectId: Long,
    @RequestBody @Valid request: ProjectMentionSettingRequest,
  ) {
    notificationMentionMuteService.setForUser(authenticationFacade.authenticatedUser.id, projectId, request.muted)
  }
}
