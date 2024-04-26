package io.tolgee.api.v2.controllers.slack

import com.slack.api.Slack
import com.slack.api.methods.request.team.TeamInfoRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.component.automations.processors.slackIntegration.SlackUserLoginUrlProvider
import io.tolgee.configuration.tolgee.SlackProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.SlackUserInfoModel
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.util.Logging
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/slack"])
@Tag(
  name = "Slack user login",
  description = "Connects Slack account with user account in Tolgee",
)
class SlackLoginController(
  private val slackUserConnectionService: SlackUserConnectionService,
  private val slackExecutor: SlackExecutor,
  private val authenticationFacade: AuthenticationFacade,
  private val slackUserLoginUrlProvider: SlackUserLoginUrlProvider,
  private val slackClient: Slack,
  private val slackProperties: SlackProperties,
  private val slackWorkspaceService: OrganizationSlackWorkspaceService,
  private val organizationRoleService: OrganizationRoleService,
) : Logging {
  @PostMapping("/user-login")
  @Operation(summary = "User login", description = "Pairs user account with slack account.")
  fun userLogin(
    @Parameter(description = "The encrypted data about the desired connection between Slack account and Tolgee account")
    @RequestParam data: String,
  ) {
    val decrypted = slackUserLoginUrlProvider.decryptData(data)
    val token = getToken(decrypted.workspaceId)
    slackUserConnectionService.createOrUpdate(authenticationFacade.authenticatedUserEntity, decrypted.slackUserId)
    slackExecutor.sendUserLoginSuccessMessage(token, decrypted)
  }

  @GetMapping("/user-login-info")
  @Operation(
    summary = "Returns connection info",
    description =
      "Returns information about the connection between " +
        "Slack account and Tolgee account which user is performing. The flow is the following. " +
        "\n\n1. User executes slash command " +
        "in Slack and gets link with encrypted Slack user ID, workspace ID and the Channel ID " +
        "(to send success response to)" +
        "\n\n" +
        "2. User gets opens the link and the Tolgee Platform frontend and it uses this endpoint " +
        "to get the data info about the future connection",
  )
  fun getInfo(
    @Parameter(description = "The encrypted data")
    @RequestParam data: String,
  ): SlackUserInfoModel {
    val decrypted = slackUserLoginUrlProvider.decryptData(data)
    val token = getToken(decrypted.workspaceId)
    val teamInfo = slackClient.methods(token).teamInfo(TeamInfoRequest.builder().token(token).build())
    val userInfo = slackClient.methods(token).usersInfo(UsersInfoRequest.builder().user(decrypted.slackUserId).build())

    if (!teamInfo.isOk || !userInfo.isOk) {
      val errResponse =
        if (!teamInfo.isOk) {
          teamInfo
        } else {
          userInfo
        }
      if (errResponse.error == "missing_scope") {
        throw BadRequestException(Message.SLACK_MISSING_SCOPE, listOf(errResponse.needed))
      }
      throw BadRequestException(Message.CANNOT_FETCH_USER_DETAILS_FROM_SLACK, listOf(errResponse.error))
    }

    return SlackUserInfoModel(
      teamName = teamInfo.team.name,
      slackId = userInfo.user.id,
      slackName = userInfo.user.name,
      slackRealName = userInfo.user.realName,
      slackAvatar = userInfo.user.profile.image72,
    )
  }

  /**
   * This method also checks the permissions
   */
  private fun getToken(workspaceId: Long?): String {
    val workspaceIdNotNull =
      workspaceId ?: return slackProperties.token ?: throw BadRequestException(Message.SLACK_NOT_CONFIGURED)

    val workspace = slackWorkspaceService.get(workspaceIdNotNull)
    organizationRoleService.checkUserCanView(workspace.organization.id)

    return workspace.accessToken
  }
}
