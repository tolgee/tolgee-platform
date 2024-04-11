package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.constants.Message
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.dtos.request.slack.SlackConnectionDto
import io.tolgee.dtos.request.slack.SlackEventDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.service.slackIntegration.SlackWorkspaceNotFound
import io.tolgee.util.I18n
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/slack"])
class SlackIntegrationController(
  private val projectService: ProjectService,
  private val slackConfigService: SlackConfigService,
  private val slackUserConnectionService: SlackUserConnectionService,
  private val slackExecutor: SlackExecutor,
  private val permissionService: PermissionService,
  private val objectMapper: ObjectMapper,
  private val i18n: I18n,
  private val authenticationFacade: AuthenticationFacade,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
  private val organizationRoleService: OrganizationRoleService,
) : Logging {
  @PostMapping
  fun slackCommand(
    @ModelAttribute payload: SlackCommandDto,
  ): SlackMessageDto? {
    val matchResult = commandRegex.matchEntire(payload.text) ?: return getError(payload, Message.SLACK_INVALID_COMMAND)

    val (command, projectId, languageTag, optionsString) = matchResult.destructured

    val optionsMap = parseOptions(optionsString)

    return when (command) {
      "login" -> login(payload)

      "subscribe" -> handleSubscribe(payload, projectId, languageTag, optionsMap)

      "unsubscribe" -> unsubscribe(payload, projectId)

      "subscriptions" -> listOfSubscriptions(payload)

      "help" -> slackExecutor.sendHelpMessage(payload.channel_id, payload.team_id)

      else -> {
        getError(payload, Message.SLACK_INVALID_COMMAND)
      }
    }
  }

  private fun listOfSubscriptions(payload: SlackCommandDto): SlackMessageDto {
    slackUserConnectionService.findBySlackId(payload.user_id)
      ?: return getError(payload, Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT)

    if (slackConfigService.getAllByChannelId(payload.channel_id).isEmpty()) {
      return getError(payload, Message.SLACK_NOT_SUBSCRIBED_YET)
    }

    return slackExecutor.getListOfSubscriptions(payload.user_id, payload.channel_id)
  }

  private fun login(payload: SlackCommandDto): SlackMessageDto? {
    if (slackUserConnectionService.isUserConnected(payload.user_id)) {
      return SlackMessageDto(text = i18n.translate("already_logged_in"))
    }

    val workspace = organizationSlackWorkspaceService.findBySlackTeamId(payload.team_id)

    slackExecutor.sendRedirectUrl(payload.channel_id, payload.user_id, workspace)
    return null
  }

  fun handleSubscribe(
    payload: SlackCommandDto,
    projectId: String,
    languageTag: String?,
    optionsMap: Map<String, String>,
  ): SlackMessageDto? {
    if (projectId.isEmpty()) {
      return getError(payload, Message.SLACK_INVALID_COMMAND)
    }

    var onEvent: EventName? = null
    optionsMap.forEach { (option, value) ->
      when (option) {
        "--on" ->
          onEvent =
            try {
              EventName.valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
              getError(payload, Message.SLACK_INVALID_COMMAND)
              return null
            }
        else -> {
          getError(payload, Message.SLACK_INVALID_COMMAND)
          return null
        }
      }
    }

    return subscribe(payload, projectId, languageTag, onEvent)
  }

  private fun subscribe(
    payload: SlackCommandDto,
    projectId: String,
    languageTag: String?,
    onEventName: EventName?,
  ): SlackMessageDto? {
    val validationResult = validateRequest(payload, projectId)
    if (!validationResult.success) {
      return null
    }

    val slackConfigDto =
      SlackConfigDto(
        project = validationResult.project,
        slackId = payload.user_id,
        channelId = payload.channel_id,
        userAccount = validationResult.user,
        languageTag = languageTag,
        onEvent = onEventName,
        slackTeamId = payload.team_id,
      )

    try {
      slackConfigService.create(slackConfigDto)
    } catch (e: Exception) {
      when (e) {
        is SlackWorkspaceNotFound -> {
          return getWorkspaceNotFoundError()
        }
      }
      logger.info(e.message)
      return getError(payload, Message.UNEXPECTED_ERROR_SLACK)
    }

    return SlackMessageDto(text = i18n.translate("subscribed_successfully"))
  }

  private fun unsubscribe(
    payload: SlackCommandDto,
    projectId: String,
  ): SlackMessageDto? {
    val validationResult = validateRequest(payload, projectId)
    if (!validationResult.success) {
      return null
    }

    if (!slackConfigService.delete(validationResult.project.id, payload.channel_id)) {
      return getError(payload, Message.SLACK_NOT_SUBSCRIBED_YET)
    }

    return SlackMessageDto(
      text = i18n.translate("unsubscribed-successfully"),
    )
  }

  @PostMapping("/on-event")
  @Transactional
  fun fetchEvent(
    @RequestBody payload: String,
  ): SlackMessageDto? {
    val decodedPayload = URLDecoder.decode(payload.substringAfter("="), "UTF-8")
    val event: SlackEventDto = objectMapper.readValue(decodedPayload)

    event.actions.forEach { action ->
      if (action.value != "help_btn") {
        return@forEach
      } else {
        slackExecutor.sendHelpMessage(event.channel.id, event.team.id)
      }
    }

    return null
  }

  @DeleteMapping("/organizations/{organizationId}")
  fun deleteOrganisationLink(
    @PathVariable
    organizationId: Long,
  ) {
    organizationSlackWorkspaceService.delete(organizationId)
  }

  private fun validateRequest(
    payload: SlackCommandDto,
    projectId: String,
  ): ValidationResult {
    val slackSubscription = slackUserConnectionService.findBySlackId(payload.user_id)

    if (slackSubscription == null) {
      getError(payload, Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT)
      return ValidationResult(false)
    }

    val id = projectId.toLongOrNull()

    if (id == null) {
      getError(payload, Message.SLACK_INVALID_COMMAND)
      return ValidationResult(false)
    }

    val project = projectService.find(id) ?: return ValidationResult(false)
    val userAccount = slackSubscription.userAccount ?: return ValidationResult(false)

    return if (permissionService.getProjectPermissionScopes(project.id, userAccount.id)
        ?.contains(Scope.ACTIVITY_VIEW) == true
    ) {
      ValidationResult(true, user = userAccount, project = project)
    } else {
      ValidationResult(false)
    }
  }

  private fun getError(
    payload: SlackCommandDto,
    message: Message,
  ): SlackMessageDto {
    val workspace = organizationSlackWorkspaceService.findBySlackTeamId(payload.team_id)

    return slackExecutor.getErrorMessage(
      message,
      payload,
      workspace,
    )
  }

  private fun getWorkspaceNotFoundError(): SlackMessageDto {
    return slackExecutor.getWorkspaceNotFoundError()
  }

  fun parseOptions(optionsString: String): Map<String, String> {
    val optionsMap = mutableMapOf<String, String>()

    optionsRegex.findAll(optionsString).forEach { match ->
      val (key, value) = match.destructured
      optionsMap[key] = value
    }

    return optionsMap
  }

  @PostMapping("/user-login")
  fun userLogin(
    @RequestBody payload: SlackConnectionDto,
  ) {
    val workspace = organizationSlackWorkspaceService.get(payload.workspaceId)

    organizationRoleService.checkUserCanView(workspace.organization.id)

    slackUserConnectionService.createOrUpdate(authenticationFacade.authenticatedUserEntity, payload.slackId)
    slackExecutor.sendSuccessMessage(payload)
  }

  companion object {
    val commandRegex = """^(\w+)(?:\s+(\d+))?(?:\s+(\w{2}))?\s*(.*)$""".toRegex()
    val optionsRegex = """(--[\w-]+)\s+([\w-]+)""".toRegex()
  }

  data class ValidationResult(
    val success: Boolean,
    val user: UserAccount = UserAccount(),
    val project: Project = Project(),
  )
}
