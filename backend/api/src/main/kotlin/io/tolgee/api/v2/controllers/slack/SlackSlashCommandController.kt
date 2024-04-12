package io.tolgee.api.v2.controllers.slack

import com.slack.api.model.block.LayoutBlock
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.SlackRequestValidation
import io.tolgee.component.automations.processors.slackIntegration.SlackErrorProvider
import io.tolgee.component.automations.processors.slackIntegration.SlackExceptionHandler
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.component.automations.processors.slackIntegration.SlackHelpBlocksProvider
import io.tolgee.component.automations.processors.slackIntegration.asSlackMessageDto
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.exceptions.SlackErrorException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.service.slackIntegration.SlackWorkspaceNotFound
import io.tolgee.util.I18n
import io.tolgee.util.Logging
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
  private val slackUserConnectionService: SlackUserConnectionService,
  private val slackExecutor: SlackExecutor,
  private val permissionService: PermissionService,
  private val i18n: I18n,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
  private val slackRequestValidation: SlackRequestValidation,
  private val slackErrorProvider: SlackErrorProvider,
  private val slackExceptionHandler: SlackExceptionHandler,
  private val slackHelpBlocksProvider: SlackHelpBlocksProvider,
) : Logging {
  @Suppress("UastIncorrectHttpHeaderInspection")
  @PostMapping
  fun slackCommand(
    @ModelAttribute payload: SlackCommandDto,
    @RequestHeader("X-Slack-Signature") slackSignature: String,
    @RequestHeader("X-Slack-Request-Timestamp") timestamp: String,
    @RequestBody body: String,
  ): SlackMessageDto? {
    return slackExceptionHandler.handle {
      slackRequestValidation.validate(slackSignature, timestamp, body)

      val matchResult =
        commandRegex.matchEntire(payload.text) ?: throw SlackErrorException(slackErrorProvider.getInvalidCommandError())

      val (command, projectId, languageTag, optionsString) = matchResult.destructured

      val optionsMap = parseOptions(optionsString)

      when (command) {
        "login" -> login(payload)

        "subscribe" -> handleSubscribe(payload, projectId.toLongOrThrowInvalidCommand(), languageTag, optionsMap)

        "unsubscribe" -> unsubscribe(payload, projectId.toLongOrThrowInvalidCommand())

        "subscriptions" -> listOfSubscriptions(payload).asSlackMessageDto

        "help" -> slackHelpBlocksProvider.getHelpBlocks().asSlackMessageDto

        "logout" -> logout(payload.user_id)

        else -> {
          throw SlackErrorException(slackErrorProvider.getInvalidCommandError())
        }
      }
    }
  }

  private fun String?.toLongOrThrowInvalidCommand(): Long {
    return this?.toLongOrNull() ?: throw SlackErrorException(slackErrorProvider.getInvalidCommandError())
  }

  private fun logout(slackId: String): SlackMessageDto {
    if (!slackUserConnectionService.delete(slackId)) {
      return SlackMessageDto(text = "Not logged in")
    }

    return SlackMessageDto(text = "Logged out")
  }

  private fun listOfSubscriptions(payload: SlackCommandDto): List<LayoutBlock> {
    slackUserConnectionService.findBySlackId(payload.user_id)
      ?: throw SlackErrorException(slackErrorProvider.getUserNotConnectedError(payload))

    if (slackConfigService.getAllByChannelId(payload.channel_id).isEmpty()) {
      throw SlackErrorException(slackErrorProvider.getNotSubscribedYetError())
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
    projectId: Long,
    languageTag: String?,
    optionsMap: Map<String, String>,
  ): SlackMessageDto? {
    var onEvent: EventName? = null
    optionsMap.forEach { (option, value) ->
      when (option) {
        "--on" ->
          onEvent =
            try {
              EventName.valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
              throw SlackErrorException(slackErrorProvider.getInvalidCommandError())
            }
        else -> {
          throw SlackErrorException(slackErrorProvider.getInvalidCommandError())
        }
      }
    }

    return subscribe(payload, projectId, languageTag, onEvent)
  }

  private fun subscribe(
    payload: SlackCommandDto,
    projectId: Long,
    languageTag: String?,
    onEventName: EventName?,
  ): SlackMessageDto {
    val user = getUserAccount(payload)
    checkPermissions(projectId, userAccountId = user.id)

    val slackConfigDto =
      SlackConfigDto(
        project = getProject(projectId),
        slackId = payload.user_id,
        channelId = payload.channel_id,
        userAccount = user,
        languageTag = languageTag,
        onEvent = onEventName,
        slackTeamId = payload.team_id,
      )

    try {
      slackConfigService.create(slackConfigDto)
    } catch (e: SlackWorkspaceNotFound) {
      throw SlackErrorException(slackErrorProvider.getWorkspaceNotFoundError())
    }

    return SlackMessageDto(text = i18n.translate("subscribed_successfully"))
  }

  private fun unsubscribe(
    payload: SlackCommandDto,
    projectId: Long,
  ): SlackMessageDto {
    val user = getUserAccount(payload)
    checkPermissions(projectId, user.id)

    if (!slackConfigService.delete(projectId, payload.channel_id)) {
      throw SlackErrorException(slackErrorProvider.getNotSubscribedYetError())
    }

    return SlackMessageDto(
      text = i18n.translate("unsubscribed-successfully"),
    )
  }

  private fun checkPermissions(
    projectId: Long,
    userAccountId: Long,
  ) {
    if (
      permissionService.getProjectPermissionScopes(projectId, userAccountId)
        ?.contains(Scope.ACTIVITY_VIEW) != true
    ) {
      throw SlackErrorException(slackErrorProvider.getNoPermissionError())
    }
  }

  private fun getProject(id: Long): Project {
    return projectService.find(id) ?: throw SlackErrorException(slackErrorProvider.getProjectNotFoundError())
  }

  private fun getUserAccount(payload: SlackCommandDto): UserAccount {
    val slackUserConnection =
      slackUserConnectionService.findBySlackId(payload.user_id)
        ?: throw SlackErrorException(slackErrorProvider.getUserNotConnectedError(payload))

    return slackUserConnection.userAccount
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
}
