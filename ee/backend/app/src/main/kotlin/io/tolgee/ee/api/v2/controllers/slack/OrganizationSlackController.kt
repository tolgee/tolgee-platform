/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.ee.api.v2.controllers.slack

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.SlackProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.dtos.request.ConnectToSlackDto
import io.tolgee.ee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.organization.slack.ConnectToSlackUrlModel
import io.tolgee.hateoas.organization.slack.WorkspaceModel
import io.tolgee.hateoas.organization.slack.WorkspaceModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresOrganizationRole
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/slack"])
@Tag(name = "Organization Slack")
class OrganizationSlackController(
  private val organizationHolder: OrganizationHolder,
  private val slackProperties: SlackProperties,
  private val slackWorkspaceService: OrganizationSlackWorkspaceService,
  private val authenticationFacade: AuthenticationFacade,
  private val workspaceModelAssembler: WorkspaceModelAssembler,
) {
  @GetMapping("get-connect-url")
  @Operation(
    summary = "Get connect URL for Slack authentication",
    description = "Returns URL to which user should be redirected to connect Slack workspace",
  )
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun connectToSlack(
    @PathVariable organizationId: Long,
  ): ConnectToSlackUrlModel {
    val organization = organizationHolder.organization
    return ConnectToSlackUrlModel(
      "https://slack.com/oauth/v2/authorize" +
        "?client_id=${slackProperties.clientId}" +
        "&scope=channels:read,chat:write,commands,users:read,team:read,groups:read,groups:write,im:read,im:write," +
        "mpim:write,users:read.email&user_scope=" +
        "&redirect_uri=${slackWorkspaceService.getRedirectUrl(organization.slug)}",
    )
  }

  @PostMapping("/connect")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Connect Slack workspace to organization",
    description =
      "This endpoint allows the owner of an organization to connect a Slack workspace to their organization.\n" +
        "Checks if the Slack integration feature is enabled for the organization and proceeds with the connection.",
  )
  @RequiresFeatures(Feature.SLACK_INTEGRATION)
  fun connectWorkspace(
    @RequestBody data: ConnectToSlackDto,
    @PathVariable organizationId: Long,
  ) {
    try {
      slackWorkspaceService.connect(
        data = data,
        organization = organizationHolder.organizationEntity,
        userAccount = authenticationFacade.authenticatedUserEntity,
      )
    } catch (e: DataIntegrityViolationException) {
      throw BadRequestException(Message.SLACK_WORKSPACE_ALREADY_CONNECTED)
    }
  }

  @GetMapping("/workspaces")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Get connected workspaces",
    description = "Returns a list of workspaces connected to the organization",
  )
  fun getConnectedWorkspaces(
    @PathVariable organizationId: Long,
  ): CollectionModel<WorkspaceModel> {
    val workspaces = slackWorkspaceService.findAllWorkspaces(organizationId)
    return workspaceModelAssembler.toCollectionModel(workspaces)
  }

  @DeleteMapping("/workspaces/{workspaceId}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Disconnect workspace",
    description = "Disconnects a workspace from the organization",
  )
  fun disconnectWorkspace(
    @PathVariable workspaceId: Long,
    @PathVariable organizationId: Long,
  ) {
    slackWorkspaceService.disconnect(organizationHolder.organization.id, workspaceId)
  }
}
