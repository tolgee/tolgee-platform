package io.tolgee.api.v2.controllers

import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.constants.Message
import io.tolgee.dtos.request.SlackCommandDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.model.Project
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserCredentialsService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackSubscriptionService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/slack/events"])
class SlackIntegrationController(
  private val projectService: ProjectService,
  private val slackConfigService: SlackConfigService,
  private val userCredentialsService: UserCredentialsService,
  private val slackSubscriptionService: SlackSubscriptionService,
  private val slackExecutor: SlackExecutor,
  private val permissionService: PermissionService,
) {

  @PostMapping("/login")
  @UseDefaultPermissions
  @AllowApiAccess
  fun login(
    @ModelAttribute payload: SlackCommandDto
  ): SlackMessageDto {
    val parts = payload.text.split(" ")
    if (parts.size < 2) {
      return SlackMessageDto(
        text = "Input error"
      )
    }

    val login = parts[0]
    val password = parts[1]

    val user = userCredentialsService.checkUserCredentials(login, password)
    slackSubscriptionService.create(
      user,
      payload.userId
    )

    return SlackMessageDto(
      text = "Success"
    )
  }

  @PostMapping("/subscribe")
  @UseDefaultPermissions
  @AllowApiAccess
  fun subscribe(
    @ModelAttribute payload: SlackCommandDto
  ): SlackMessageDto? {
    val preparationResult = prepareForSlackOperation(payload) ?: return null

    slackConfigService.create(project = preparationResult.project, payload)
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
    val preparationResult = prepareForSlackOperation(payload) ?: return null

    slackConfigService.delete(preparationResult.project.id, payload.channel_id)
    return SlackMessageDto(
      text = "unsubscribed"
    )
  }

  @PostMapping("/event")
  @UseDefaultPermissions
  fun fetchEvent(
    @RequestBody payload: String
  ): SlackMessageDto? {




    return null
  }

  private fun prepareForSlackOperation(payload: SlackCommandDto): OperationPreparationResult? {
    val slackSubscription = slackSubscriptionService.getBySlackId(payload.userId, payload.channel_id)

    if(slackSubscription == null) {
      slackExecutor.sendErrorMessage(Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT, payload.channel_id)
      return null
    }

    val project = projectService.find(payload.text.toLong()) ?: return null
    val tolgeeId = slackSubscription.userAccount?.id ?: return null

    val canViewTranslations = permissionService.getProjectPermissionScopes(project.id, tolgeeId)
      ?.contains(Scope.ACTIVITY_VIEW) == true

    if (!canViewTranslations) return null

    return OperationPreparationResult(project)
  }

  data class OperationPreparationResult(
    val project: Project
  )
}
