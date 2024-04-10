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
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackSubscriptionService
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
  private val slackSubscriptionService: SlackSubscriptionService,
  private val slackExecutor: SlackExecutor,
  private val permissionService: PermissionService,
  private val userAccountService: UserAccountService,
  private val objectMapper: ObjectMapper,
  private val i18n: I18n,
  private val authenticationFacade: AuthenticationFacade,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
) : Logging {
  @PostMapping
  @UseDefaultPermissions
  @AllowApiAccess
  fun slackCommand(
    @ModelAttribute payload: SlackCommandDto,
  ): SlackMessageDto? {
    val matchResult = commandRegex.matchEntire(payload.text)

    if (matchResult == null) {
      sendError(payload, Message.SLACK_INVALID_COMMAND)
      return null
    }

    val (command, projectId, languageTag, optionsString) = matchResult.destructured

    val optionsMap = parseOptions(optionsString)

    when (command) {
      "login" -> return login(payload)

      "subscribe" -> return handleSubscribe(payload, projectId, languageTag, optionsMap)

      "unsubscribe" -> return unsubscribe(payload, projectId)

      "subscriptions" -> listOfSubscriptions(payload)

      "help" -> help(payload.channelId)

      else -> {
        sendError(payload, Message.SLACK_INVALID_COMMAND)
        return null
      }
    }
    return null
  }

  private fun help(channelId: String) {
    slackExecutor.sendHelpMessage(channelId)
  }

  private fun listOfSubscriptions(payload: SlackCommandDto) {
    val slackSubscription = slackSubscriptionService.getBySlackId(payload.userId)

    if (slackSubscription == null) {
      sendError(payload, Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT)
      return
    }

    if (slackConfigService.get(payload.userId, payload.channelId).isEmpty()) {
      sendError(payload, Message.SLACK_NOT_SUBSCRIBED_YET)
      return
    }

    slackExecutor.sendListOfSubscriptions(payload.userId, payload.channelId)
  }

  @PostMapping("/connect")
  @UseDefaultPermissions
  fun connectSlack(
    @RequestBody payload: SlackConnectionDto,
  ) {
    if (payload.userAccountId.toLongOrNull() != authenticationFacade.authenticatedUser.id) {
      throw Exception()
    }

    val user = userAccountService.get(payload.userAccountId.toLong())
    slackSubscriptionService.create(user, payload.slackId, payload.slackNickName)

    slackExecutor.sendSuccessMessage(payload.channelId)
  }

  private fun login(payload: SlackCommandDto): SlackMessageDto? {
    if (slackSubscriptionService.ifSlackConnected(payload.userId)) {
      return SlackMessageDto(text = i18n.translate("already_logged_in"))
    }

    slackExecutor.sendRedirectUrl(payload.channelId, payload.userId, payload.userName)
    return null
  }

  fun handleSubscribe(
    payload: SlackCommandDto,
    projectId: String,
    languageTag: String?,
    optionsMap: Map<String, String>,
  ): SlackMessageDto? {
    if (projectId.isEmpty()) {
      sendError(payload, Message.SLACK_INVALID_COMMAND)
      return null
    }

    var onEvent: EventName? = null
    optionsMap.forEach { (option, value) ->
      when (option) {
        "--on" ->
          onEvent =
            try {
              EventName.valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
              sendError(payload, Message.SLACK_INVALID_COMMAND)
              return null
            }
        else -> {
          sendError(payload, Message.SLACK_INVALID_COMMAND)
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
        slackId = payload.userId,
        channelId = payload.channelId,
        userAccount = validationResult.user,
        languageTag = languageTag,
        onEvent = onEventName,
        slackTeamId = payload.teamId,
      )

    try {
      slackConfigService.create(slackConfigDto)
    } catch (e: Exception) {
      logger.info(e.message)
      sendError(payload, Message.UNEXPECTED_ERROR_SLACK)
      return null
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

    if (!slackConfigService.delete(validationResult.project.id, payload.channelId)) {
      sendError(payload, Message.SLACK_NOT_SUBSCRIBED_YET)
      return null
    }

    return SlackMessageDto(
      i18n.translate("unsubscribed-successfully"),
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
        slackExecutor.sendHelpMessage(event.channel.id)
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
    val slackSubscription = slackSubscriptionService.getBySlackId(payload.userId)

    if (slackSubscription == null) {
      sendError(payload, Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT)
      return ValidationResult(false)
    }

    val id = projectId.toLongOrNull()

    if (id == null) {
      sendError(payload, Message.SLACK_INVALID_COMMAND)
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

  private fun sendError(
    payload: SlackCommandDto,
    message: Message,
  ) {
    val workspace = organizationSlackWorkspaceService.findBySlackTeamId(payload.teamId)

    slackExecutor.sendErrorMessage(
      message,
      payload,
      workspace,
    )
  }

  fun parseOptions(optionsString: String): Map<String, String> {
    val optionsMap = mutableMapOf<String, String>()

    optionsRegex.findAll(optionsString).forEach { match ->
      val (key, value) = match.destructured
      optionsMap[key] = value
    }

    return optionsMap
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
