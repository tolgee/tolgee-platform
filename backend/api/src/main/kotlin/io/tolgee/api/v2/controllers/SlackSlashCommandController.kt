package io.tolgee.api.v2.controllers

import com.esotericsoftware.minlog.Log
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.SlackRequestValidation
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.constants.Message
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.dtos.request.slack.SlackConnectionDto
import io.tolgee.dtos.response.OrgToWorkspaceLinkDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.slackIntegration.OrgToWorkspaceLinkService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackSubscriptionService
import io.tolgee.util.I18n
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.web.bind.annotation.*
import kotlin.jvm.optionals.getOrElse

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/public/slack"])
@Tag(
  name = "Slack slack commands",
  description = "Processes Slack slash commands, enabling users to execute specific actions within Slack",
)
class SlackSlashCommandController(
  private val projectService: ProjectService,
  private val slackConfigService: SlackConfigService,
  private val slackSubscriptionService: SlackSubscriptionService,
  private val slackExecutor: SlackExecutor,
  private val permissionService: PermissionService,
  private val i18n: I18n,
  private val authenticationFacade: AuthenticationFacade,
  private val orgToWorkspaceLinkService: OrgToWorkspaceLinkService,
  private val organizationService: OrganizationService,
  private val slackRequestValidation: SlackRequestValidation,
) : Logging {
  @PostMapping
  fun slashCommand(
    @ModelAttribute payload: SlackCommandDto,
    @RequestHeader("X-Slack-Signature") slackSignature: String,
    @RequestHeader("X-Slack-Request-Timestamp") timestamp: String,
    @RequestBody body: String,
  ): SlackMessageDto? {
    if (!slackRequestValidation.isValid(slackSignature, timestamp, body)) {
      Log.info("Error validating request from Slack")
      throw BadRequestException(Message.UNEXPECTED_ERROR_SLACK)
    }

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

      "help" -> help(payload.channel_id)

      "logout" -> return logout(payload.user_id)

      else -> {
        sendError(payload, Message.SLACK_INVALID_COMMAND)
        return null
      }
    }
    return null
  }

  private fun logout(slackId: String): SlackMessageDto {
    if (!slackSubscriptionService.delete(slackId)) {
      return SlackMessageDto(text = "Not logged in")
    }

    return if (!slackConfigService.deleteAllBySlackId(slackId)) {
      SlackMessageDto(text = "Cant logout")
    } else {
      SlackMessageDto(text = "Logged out")
    }
  }

  private fun help(channelId: String) {
    slackExecutor.sendHelpMessage(channelId)
  }

  private fun listOfSubscriptions(payload: SlackCommandDto) {
    val slackSubscription = slackSubscriptionService.getBySlackId(payload.user_id)

    if (slackSubscription == null) {
      sendError(payload, Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT)
      return
    }

    if (slackConfigService.get(payload.user_id, payload.channel_id).isEmpty()) {
      sendError(payload, Message.SLACK_NOT_SUBSCRIBED_YET)
      return
    }

    slackExecutor.sendListOfSubscriptions(payload.user_id, payload.channel_id)
  }

  @PostMapping("/connect")
  fun connectSlack(
    @RequestBody payload: SlackConnectionDto,
  ) {
    if (payload.workSpace.isNotBlank() && !orgToWorkspaceLinkService.ifWorkSpaceLinked(payload.workSpace)) {
      val organization = organizationService.get(payload.orgId.toLong())
      orgToWorkspaceLinkService.save(
        organization,
        payload.workSpace,
        payload.channelName,
        payload.author,
        payload.workSpaceName,
      )
      slackExecutor.sendSuccessMessage(payload.channelId)
      slackExecutor.sendRedirectUrl(payload.channelId, payload.slackId, payload.slackNickName)
      return
    }
    val user = authenticationFacade.authenticatedUserEntityOrNull ?: throw BadRequestException(Message.UNAUTHENTICATED)

    slackSubscriptionService.create(user, payload.slackId, payload.slackNickName)

    slackExecutor.sendSuccessMessage(payload.channelId)
  }

  private fun login(payload: SlackCommandDto): SlackMessageDto? {
    if (!orgToWorkspaceLinkService.ifWorkSpaceLinked(payload.team_id)) {
      slackExecutor.connectOrganisationButton(
        payload.channel_id,
        payload.team_id,
        payload.user_id,
        payload.user_name,
        payload.channel_name,
        payload.team_domain,
      )
      return null
    }

    if (slackSubscriptionService.ifSlackConnected(payload.user_id)) {
      return SlackMessageDto(text = i18n.translate("already_logged_in"))
    }

    slackExecutor.sendRedirectUrl(payload.channel_id, payload.user_id, payload.user_name)
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
        slackId = payload.user_id,
        channelId = payload.channel_id,
        userAccount = validationResult.user,
        languageTag = languageTag,
        onEvent = onEventName,
        workSpaceId = payload.team_id,
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

    if (!slackConfigService.delete(validationResult.project.id, payload.channel_id)) {
      sendError(payload, Message.SLACK_NOT_SUBSCRIBED_YET)
      return null
    }

    return SlackMessageDto(
      i18n.translate("unsubscribed-successfully"),
    )
  }

  @GetMapping("/organizations/{organizationId}/is-paired")
  fun isOrgPaired(
    @PathVariable
    organizationId: Long,
  ): Boolean {
    return !orgToWorkspaceLinkService.findByOrgIdOptional(organizationId).isEmpty
  }

  @GetMapping("/organizations/{organizationId}")
  fun getLinkedOrganisations(
    @PathVariable
    organizationId: Long,
  ): OrgToWorkspaceLinkDto {
    val orgToWorkspaceLink =
      orgToWorkspaceLinkService.findByOrgIdOptional(
        organizationId,
      ).getOrElse { throw NotFoundException() }
    return OrgToWorkspaceLinkDto.fromEntity(orgToWorkspaceLink)
  }

  @DeleteMapping("/organizations/{organizationId}")
  fun deleteOrganisationLink(
    @PathVariable
    organizationId: Long,
  ) {
    orgToWorkspaceLinkService.delete(organizationId)
  }

  private fun validateRequest(
    payload: SlackCommandDto,
    projectId: String,
  ): ValidationResult {
    if (!orgToWorkspaceLinkService.ifWorkSpaceLinked(payload.team_id)) {
      sendError(payload, Message.SLACK_NOT_LINKED_ORG)
      return ValidationResult(false)
    }

    val slackSubscription = slackSubscriptionService.getBySlackId(payload.user_id)

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
    slackExecutor.sendErrorMessage(
      message,
      payload.channel_id,
      payload.user_id,
      payload.user_name,
      payload.team_id,
      payload.channel_name,
      payload.team_domain,
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
