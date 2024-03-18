package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.activity.ActivityHolder
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.constants.Message
import io.tolgee.constants.SlackEventActions
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
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackSubscriptionService
import io.tolgee.service.translation.TranslationService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
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
  private val translationService: TranslationService,
  private val keyService: KeyService,
  private val activityHolder: ActivityHolder,
  private val objectMapper: ObjectMapper,
) {
  @PostMapping
  @UseDefaultPermissions
  @AllowApiAccess
  fun slackCommand(
    @ModelAttribute payload: SlackCommandDto,
  ): SlackMessageDto? {
    val regex = """^(\w+)(?:\s+(\d+))?(?:\s+(\w{2}))?\s*(.*)$""".toRegex()
    val matchResult = regex.matchEntire(payload.text)

    if (matchResult == null) {
      slackExecutor.sendErrorMessage(
        Message.SLACK_INVALID_COMMAND,
        payload.channel_id,
        payload.user_id,
      )
      return null
    }

    val (command, projectId, languageTag, optionsString) = matchResult.destructured

    val optionsMap = parseOptions(optionsString)

    return when (command) {
      "login" -> return login(payload.user_id, payload.channel_id)

      "subscribe" -> return handleSubscribe(payload, projectId, languageTag, optionsMap)

      "unsubscribe" -> return unsubscribe(payload, projectId)

      else -> {
        slackExecutor.sendErrorMessage(
          Message.SLACK_INVALID_COMMAND,
          payload.channel_id,
          payload.user_id,
        )
        null
      }
    }
  }

  @PostMapping("/connect")
  @UseDefaultPermissions
  fun connectSlack(
    @RequestBody payload: SlackConnectionDto,
  ) {
    val user = userAccountService.get(payload.userAccountId.toLong())
    slackSubscriptionService.create(user, payload.slackId)

    slackExecutor.sendSuccessMessage(payload.channelId)
  }

  private fun login(
    userId: String,
    channelId: String,
  ): SlackMessageDto? {
    if (slackSubscriptionService.ifSlackConnected(userId)) {
      return SlackMessageDto(text = "You are already logged in.")
    }

    slackExecutor.sendRedirectUrl(channelId, userId)
    return null
  }

  fun handleSubscribe(
    payload: SlackCommandDto,
    projectId: String,
    languageTag: String?,
    optionsMap: Map<String, String>,
  ): SlackMessageDto? {
    if (projectId.isEmpty()) {
      slackExecutor.sendErrorMessage(
        Message.SLACK_INVALID_COMMAND,
        payload.channel_id,
        payload.user_id,
      )
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
              return SlackMessageDto("Invalid command")
            }
        else -> {
          slackExecutor.sendErrorMessage(
            Message.SLACK_INVALID_COMMAND,
            payload.channel_id,
            payload.user_id,
          )
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
      )

    try {
      slackConfigService.create(slackConfigDto)
    } catch (e: Exception) {
      return SlackMessageDto(text = "Error")
    }

    return SlackMessageDto(text = "Subscribed")
  }

  private fun unsubscribe(
    payload: SlackCommandDto,
    projectId: String,
  ): SlackMessageDto? {
    val validationResult = validateRequest(payload, projectId)
    if (!validationResult.success) {
      return null
    }

    slackConfigService.delete(validationResult.project.id, payload.channel_id)

    return SlackMessageDto(
      text = "unsubscribed",
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
      val parameters = action.actionId.substringAfter(SlackEventActions.TRANSLATE_VALUE.name + "/")
      if (parameters == action.actionId) {
        return@forEach
      }

      val regex = "(\\d+)/([a-zA-Z-]+)".toRegex()
      val matchResult = regex.find(parameters) ?: return@forEach

      val (keyId, langName) = matchResult.destructured
      val key = keyService.get(keyId.toLong())
      val translation =
        mapOf(
          langName to action.value,
        )

      activityHolder.activityRevision.projectId = key.project.id
      translationService.setForKey(key, translation)
      slackExecutor.sendSuccessModal(event.triggerId)
    }

    return null
  }

  private fun validateRequest(
    payload: SlackCommandDto,
    projectId: String,
  ): ValidationResult {
    val slackSubscription = slackSubscriptionService.getBySlackId(payload.user_id)

    if (slackSubscription == null) {
      slackExecutor.sendErrorMessage(
        Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT,
        payload.channel_id,
        payload.user_id,
      )
      return ValidationResult(false)
    }

    val project = projectService.find(projectId.toLong()) ?: return ValidationResult(false)
    val userAccount = slackSubscription.userAccount ?: return ValidationResult(false)

    return if (permissionService.getProjectPermissionScopes(project.id, userAccount.id)
        ?.contains(Scope.ACTIVITY_VIEW) == true
    ) {
      ValidationResult(true, user = userAccount, project = project)
    } else {
      ValidationResult(false)
    }
  }

  fun parseOptions(optionsString: String): Map<String, String> {
    val optionsRegex = """(--[\w-]+)\s+([\w-]+)""".toRegex()
    val optionsMap = mutableMapOf<String, String>()

    optionsRegex.findAll(optionsString).forEach { match ->
      val (key, value) = match.destructured
      optionsMap[key] = value
    }

    return optionsMap
  }

  data class ValidationResult(
    val success: Boolean,
    val user: UserAccount = UserAccount(),
    val project: Project = Project(),
  )
}
