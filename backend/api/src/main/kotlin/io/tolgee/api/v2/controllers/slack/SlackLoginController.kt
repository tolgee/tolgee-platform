package io.tolgee.api.v2.controllers.slack

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.dtos.request.slack.SlackConnectionDto
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.util.Logging
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/slack"])
@Tag(
  name = "Slack user login",
  description = "Connects slack account with user account in Tolgee.",
)
class SlackLoginController(
  private val slackUserConnectionService: SlackUserConnectionService,
  private val slackExecutor: SlackExecutor,
  private val authenticationFacade: AuthenticationFacade,
) : Logging {
  @PostMapping("/user-login")
  @Operation(summary = "User login", description = "Pairs user account with slack account.")
  fun userLogin(
    @RequestBody payload: SlackConnectionDto,
  ) {
    slackUserConnectionService.createOrUpdate(authenticationFacade.authenticatedUserEntity, payload.slackId)
    slackExecutor.sendUserLoginSuccessMessage(payload)
  }
}
