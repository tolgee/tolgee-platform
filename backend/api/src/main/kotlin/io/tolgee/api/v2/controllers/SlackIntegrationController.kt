package io.tolgee.api.v2.controllers

import io.tolgee.dtos.request.SlackCommandDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.project.ProjectService
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
  private val slackSubscriptionService: SlackSubscriptionService
) {

  @PostMapping("/login")
  @UseDefaultPermissions
  @AllowApiAccess
  fun login(
    @ModelAttribute payload: SlackCommandDto
  ): SlackMessageDto {
    val parts = payload.text.split(" ")
    if (parts.size < 2) {
      //todo error handling
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
  ): SlackMessageDto {
    slackSubscriptionService.getBySlackId(payload.userId, payload.channel_id)

    val project = projectService.get(payload.text.toLong())

    slackConfigService.create(project = project, payload)
    return SlackMessageDto(
      text = "subscribed"
    )
    //TODO handle error
  }

  @PostMapping("/unsubscribe")
  @UseDefaultPermissions
  @AllowApiAccess
  fun unsubscribe(
    @ModelAttribute payload: SlackCommandDto
  ): SlackMessageDto {
    slackSubscriptionService.getBySlackId(payload.userId, payload.channel_id)

    slackConfigService.delete(payload.text.toLong(), payload.channel_id)
    return SlackMessageDto(
      text = "unsubscribed"
    )
    //TODO handle error
  }

}
