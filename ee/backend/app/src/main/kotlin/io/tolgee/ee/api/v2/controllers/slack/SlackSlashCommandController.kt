package io.tolgee.ee.api.v2.controllers.slack

import com.slack.api.model.block.LayoutBlock
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.component.slackIntegration.slashcommand.SlackBotInfoProvider
import io.tolgee.ee.component.slackIntegration.slashcommand.SlackErrorProvider
import io.tolgee.ee.component.slackIntegration.slashcommand.SlackExceptionHandler
import io.tolgee.ee.component.slackIntegration.slashcommand.SlackRequestValidation
import io.tolgee.ee.component.slackIntegration.slashcommand.SlackSlackCommandBlocksProvider
import io.tolgee.ee.component.slackIntegration.slashcommand.asSlackResponseString
import io.tolgee.ee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.ee.service.slackIntegration.SlackConfigManageService
import io.tolgee.ee.service.slackIntegration.SlackConfigReadService
import io.tolgee.ee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.ee.service.slackIntegration.SlackWorkspaceNotFound
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.SlackErrorException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.util.I18n
import io.tolgee.util.Logging
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/public/slack"])
@Tag(
  name = "Slack slack commands",
  description = "Processes Slack slash commands, enabling users to execute specific actions within Slack",
)
class SlackSlashCommandController(
  private val projectService: ProjectService,
  private val slackConfigManageService: SlackConfigManageService,
  private val slackConfigReadService: SlackConfigReadService,
  private val slackUserConnectionService: SlackUserConnectionService,
  private val slackBotInfoProvider: SlackBotInfoProvider,
  private val permissionService: PermissionService,
  private val i18n: I18n,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
  private val slackRequestValidation: SlackRequestValidation,
  private val slackErrorProvider: SlackErrorProvider,
  private val slackExceptionHandler: SlackExceptionHandler,
  private val slackSlackCommandBlocksProvider: SlackSlackCommandBlocksProvider,
  private val tolgeeProperties: TolgeeProperties,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) : Logging {
  @Suppress("UastIncorrectHttpHeaderInspection")
  @PostMapping
  fun slackCommand(
    @ModelAttribute payload: SlackCommandDto,
    @RequestHeader("X-Slack-Signature") slackSignature: String,
    @RequestHeader("X-Slack-Request-Timestamp") timestamp: String,
    @RequestBody body: String,
  ): String? {
    return slackExceptionHandler.handle {
      slackRequestValidation.validate(slackSignature, timestamp, body)
      val token = checkIfTokenIsPresent(payload.team_id)
      if (!slackBotInfoProvider.isBotInChannel(payload, token)) {
        throw SlackErrorException(slackErrorProvider.getBotNotInChannelError())
      }

      val matchResult =
        commandRegex.matchEntire(payload.text) ?: throw SlackErrorException(slackErrorProvider.getInvalidCommandError())

      val (command, projectId, languageTag, optionsString) = matchResult.destructured

      val optionsMap = parseOptions(optionsString)

      when (command) {
        "login" -> login(payload).asSlackResponseString

        "subscribe" ->
          handleSubscribe(
            payload,
            projectId.toLongOrThrowInvalidCommand(),
            languageTag,
            optionsMap,
          ).asSlackResponseString

        "unsubscribe" ->
          unsubscribe(
            payload,
            projectId.toLongOrThrowInvalidCommand(),
            languageTag,
          ).asSlackResponseString

        "subscriptions" -> listOfSubscriptions(payload).asSlackResponseString

        "help" -> slackSlackCommandBlocksProvider.getHelpBlocks().asSlackResponseString

        "logout" -> logout(payload.user_id, payload.team_id).asSlackResponseString
        else -> {
          throw SlackErrorException(slackErrorProvider.getInvalidCommandError())
        }
      }
    }
  }

  private fun checkIfTokenIsPresent(teamId: String): String {
    if (tolgeeProperties.slack.token != null) {
      return tolgeeProperties.slack.token!!
    }

    return organizationSlackWorkspaceService
      .findBySlackTeamId(
        teamId,
      )?.accessToken ?: throw SlackErrorException(slackErrorProvider.getWorkspaceNotFoundError())
  }

  private fun String?.toLongOrThrowInvalidCommand(): Long {
    return this?.toLongOrNull() ?: throw SlackErrorException(slackErrorProvider.getInvalidCommandError())
  }

  private fun logout(
    slackId: String,
    slackTeamId: String,
  ): SlackMessageDto {
    if (!slackUserConnectionService.delete(slackId, slackTeamId)) {
      return SlackMessageDto(text = "Not logged in")
    }

    return SlackMessageDto(text = "Logged out")
  }

  private fun listOfSubscriptions(payload: SlackCommandDto): List<LayoutBlock> {
    slackUserConnectionService.findBySlackId(payload.user_id, payload.team_id)
      ?: throw SlackErrorException(slackErrorProvider.getUserNotConnectedError(payload))

    if (slackConfigReadService.getAllByChannelId(payload.channel_id).isEmpty()) {
      throw SlackErrorException(slackErrorProvider.getNotSubscribedYetError())
    }

    return slackSlackCommandBlocksProvider.getListOfSubscriptionsBlocks(payload.user_id, payload.channel_id)
  }

  private fun login(payload: SlackCommandDto): SlackMessageDto {
    checkFeatureEnabled(payload.team_id)

    if (slackUserConnectionService.isUserConnected(payload.user_id, payload.team_id)) {
      return SlackMessageDto(text = i18n.translate("slack.common.message.already_logged_in"))
    }

    val workspace = organizationSlackWorkspaceService.findBySlackTeamId(payload.team_id)

    return SlackMessageDto(
      blocks =
        slackSlackCommandBlocksProvider.getLoginRedirectBlocks(
          payload.channel_id,
          payload.user_id,
          workspace,
          payload.team_id,
        ),
    )
  }

  fun handleSubscribe(
    payload: SlackCommandDto,
    projectId: Long,
    languageTag: String?,
    optionsMap: Map<String, String>,
  ): SlackMessageDto? {
    checkFeatureEnabled(payload.team_id, projectId)

    val events: MutableSet<SlackEventType> = mutableSetOf()

    var isGlobal: Boolean? = null
    optionsMap.forEach { (option, value) ->
      when (option) {
        "--on" -> {
          value.split(",").map { it.trim() }.forEach {
            events.add(parseEventName(it))
          }
        }
        "--global" ->
          isGlobal = value.lowercase().toBooleanStrictOrNull() ?: throw SlackErrorException(
            slackErrorProvider.getInvalidParameterError(value),
          )

        else -> {
          throw SlackErrorException(slackErrorProvider.getInvalidCommandError())
        }
      }
    }
    if (events.contains(SlackEventType.ALL)) {
      events.clear()
      events.add(SlackEventType.ALL)
    }
    return subscribe(payload, projectId, languageTag, events, isGlobal)
  }

  fun parseEventName(event: String): SlackEventType {
    return try {
      SlackEventType.valueOf(event.uppercase())
    } catch (e: IllegalArgumentException) {
      throw SlackErrorException(slackErrorProvider.getInvalidParameterError(event))
    }
  }

  private fun subscribe(
    payload: SlackCommandDto,
    projectId: Long,
    languageTag: String?,
    events: MutableSet<SlackEventType>,
    isGlobal: Boolean?,
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
        events = events,
        slackTeamId = payload.team_id,
        isGlobal = isGlobal,
      )
    try {
      val config = slackConfigManageService.createOrUpdate(slackConfigDto)
      return SlackMessageDto(
        blocks = slackSlackCommandBlocksProvider.getSuccessfullySubscribedBlocks(config),
      )
    } catch (e: SlackWorkspaceNotFound) {
      throw SlackErrorException(slackErrorProvider.getWorkspaceNotFoundError())
    }
  }

  private fun unsubscribe(
    payload: SlackCommandDto,
    projectId: Long,
    languageTag: String,
  ): SlackMessageDto {
    val user = getUserAccount(payload)
    checkPermissions(projectId, user.id)

    slackConfigManageService.delete(projectId, payload.channel_id, languageTag)

    return SlackMessageDto(
      text = i18n.translate("slack.common.message.unsubscribed-successfully"),
    )
  }

  private fun checkPermissions(
    projectId: Long,
    userAccountId: Long,
  ) {
    try {
      if (
        permissionService
          .getProjectPermissionScopesNoApiKey(projectId, userAccountId)
          ?.contains(Scope.ACTIVITY_VIEW) != true
      ) {
        throw SlackErrorException(slackErrorProvider.getNoPermissionError())
      }
    } catch (e: NotFoundException) {
      throw SlackErrorException(slackErrorProvider.getProjectNotFoundError(projectId = projectId))
    }
  }

  private fun checkFeatureEnabled(
    teamId: String,
    projectId: Long? = null,
  ) {
    val workspace = organizationSlackWorkspaceService.findBySlackTeamId(teamId)

    // this enables us to bypass the check locally for local development when billing is enabled,
    // and we are using slack with single workspace global server configuration
    // in that case workspace is null
    if (workspace == null) {
      // we prevent only project commands, which should be enough to prevent users from using it when they don't have
      // the feature enabled
      if (projectId != null) {
        val project = projectService.get(projectId)
        checkPermissionForOrganizationId(project.organizationOwner.id)
      }
      return
    }

    checkPermissionForOrganizationId(workspace.organization.id)
  }

  fun checkPermissionForOrganizationId(organizationId: Long) {
    if (!enabledFeaturesProvider.isFeatureEnabled(
        organizationId,
        Feature.SLACK_INTEGRATION,
      )
    ) {
      throw SlackErrorException(slackErrorProvider.getFeatureDisabledError())
    }
  }

  private fun getProject(id: Long): Project {
    return projectService.find(id) ?: throw SlackErrorException(slackErrorProvider.getProjectNotFoundError(id))
  }

  private fun getUserAccount(payload: SlackCommandDto): UserAccount {
    val slackUserConnection =
      slackUserConnectionService.findBySlackId(payload.user_id, payload.team_id)
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
    val commandRegex = """^(\w+)(?:\s+(\d+))?(?:\s+([\p{L}][\p{L}\d-]*))?\s*(.*)$""".toRegex()

    val optionsRegex = """(--[\w-]+)\s+([\w-,\s]+)""".toRegex()
  }
}
