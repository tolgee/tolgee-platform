package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.constants.Message
import io.tolgee.constants.SlackEventActions
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.dtos.request.slack.SlackConnectionDto
import io.tolgee.dtos.request.slack.SlackEventDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackSubscriptionService
import io.tolgee.service.translation.TranslationService
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/slack/events"])
class SlackIntegrationController(
  private val projectService: ProjectService,
  private val slackConfigService: SlackConfigService,
  private val slackSubscriptionService: SlackSubscriptionService,
  private val slackExecutor: SlackExecutor,
  private val permissionService: PermissionService,
  private val userAccountService: UserAccountService,
  private val translationService: TranslationService,
  private val keyService: KeyService,
) {

  @PostMapping("/login")
  @UseDefaultPermissions
  @AllowApiAccess
  fun login(
    @ModelAttribute payload: SlackCommandDto
  ): SlackMessageDto? {
    if(slackSubscriptionService.ifSlackConnected(payload.user_id)) {
      return SlackMessageDto(text = "You are already logged in.")
    }

    slackExecutor.sendRedirectUrl(payload.channel_id, payload.user_id)
    return null
  }

  @PostMapping("/connect")
  @UseDefaultPermissions
  fun connectSlack(
    @RequestBody payload: SlackConnectionDto
  ) {
    val user = userAccountService.get(payload.userAccountId.toLong())
    slackSubscriptionService.create(user, payload.slackId)

    slackExecutor.sendSuccessMessage(payload.channelId)
  }

  @PostMapping("/subscribe")
  @UseDefaultPermissions
  @AllowApiAccess
  fun subscribe(
    @ModelAttribute payload: SlackCommandDto
  ): SlackMessageDto? {
    val validationResult = validateRequest(payload)
    if(!validationResult.success)
      return null

    slackConfigService.create(project = validationResult.project, payload, validationResult.user)

    return SlackMessageDto(
      text = "subscribed"
    )
  }

  @PostMapping("/unsubscribe")
  @UseDefaultPermissions
  @AllowApiAccess
  fun unsubscribe(
    @ModelAttribute payload: SlackCommandDto
  ): SlackMessageDto? {

    val validationResult = validateRequest(payload)
    if(!validationResult.success)
      return null

    slackConfigService.delete(validationResult.project.id, payload.channel_id)

    return SlackMessageDto(
      text = "unsubscribed"
    )
  }

  @PostMapping("/event")
  @UseDefaultPermissions
  fun fetchEvent(
    @RequestBody payload: String
  ): SlackMessageDto? {
    val decodedPayload = URLDecoder.decode(payload.substringAfter("="), "UTF-8")
    val event: SlackEventDto = jacksonObjectMapper().readValue(decodedPayload)

    event.actions.forEach { action ->
      val parameters = action.actionId.substringAfter(SlackEventActions.TRANSLATE_VALUE.name + "/")
      if(parameters == action.actionId)
        return@forEach

      val regex = "(\\d+)/([a-zA-Z-]+)".toRegex()
      val matchResult = regex.find(parameters) ?: return@forEach

      val (keyId, langName) = matchResult.destructured
      val key = keyService.get(keyId.toLong())
      val translation = mapOf(
        langName to action.value
      )

      translationService.setForKey(key, translation)
      slackExecutor.sendSuccessModal(event.triggerId)

    }

    return null
  }

  private fun validateRequest(payload: SlackCommandDto): ValidationResult {
    val slackSubscription = slackSubscriptionService.getBySlackId(payload.user_id)

    if (slackSubscription == null) {
      slackExecutor.sendErrorMessage(Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT, payload.channel_id)
      return ValidationResult(false)
    }

    val project = projectService.find(payload.text.toLong()) ?: return ValidationResult(false)
    val userAccount = slackSubscription.userAccount ?: return ValidationResult(false)

    return if(permissionService.getProjectPermissionScopes(project.id, userAccount.id)
        ?.contains(Scope.ACTIVITY_VIEW) == true
    ) {
      ValidationResult(true, user = userAccount, project = project)
    } else {
      ValidationResult(false)
    }
  }

  data class ValidationResult(
    val success: Boolean,
    val user: UserAccount = UserAccount(),
    val project: Project = Project(),
  )
}
