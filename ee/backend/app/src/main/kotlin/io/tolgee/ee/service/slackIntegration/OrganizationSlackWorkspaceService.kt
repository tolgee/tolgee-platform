package io.tolgee.ee.service.slackIntegration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.slack.api.Slack
import com.slack.api.methods.request.apps.AppsUninstallRequest
import com.slack.api.methods.response.apps.AppsUninstallResponse
import io.sentry.Sentry
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.configuration.tolgee.SlackProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.ConnectToSlackDto
import io.tolgee.ee.repository.slackIntegration.OrganizationSlackWorkspaceRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class OrganizationSlackWorkspaceService(
  private val organizationSlackWorkspaceRepository: OrganizationSlackWorkspaceRepository,
  private val restTemplate: RestTemplate,
  private val slackProperties: SlackProperties,
  private val objectMapper: ObjectMapper,
  private val frontendUrlProvider: FrontendUrlProvider,
  private val slackClient: Slack,
) : Logging {
  @Transactional
  fun findBySlackTeamId(teamId: String): OrganizationSlackWorkspace? {
    return organizationSlackWorkspaceRepository.findBySlackTeamId(teamId)
  }

  @Transactional
  fun findAllWorkspaces(organizationId: Long): List<OrganizationSlackWorkspace> {
    return organizationSlackWorkspaceRepository.findAllByOrganizationId(organizationId)
  }

  @Transactional
  fun delete(organizationId: Long) {
    val organizationToWorkspaceLink = findAllWorkspaces(organizationId)
    organizationSlackWorkspaceRepository.deleteAll(organizationToWorkspaceLink)
  }

  @Transactional
  fun connect(
    data: ConnectToSlackDto,
    organization: Organization,
    userAccount: UserAccount,
  ) {
    val connectToSlackResponse = getConnectToSlackResponse(data, organization)
    connect(organization, connectToSlackResponse, userAccount)
  }

  private fun getConnectToSlackResponse(
    data: ConnectToSlackDto,
    organization: Organization,
  ): ConnectToSlackResponse {
    val clientId = slackProperties.clientId ?: throw BadRequestException(Message.SLACK_NOT_CONFIGURED)
    val clientSecret = slackProperties.clientSecret ?: throw BadRequestException(Message.SLACK_NOT_CONFIGURED)

    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
    headers.accept = listOf(MediaType.APPLICATION_JSON)

    val request =
      HttpEntity(
        LinkedMultiValueMap(
          mapOf(
            "code" to listOf(data.code),
            "client_id" to listOf(clientId),
            "client_secret" to listOf(clientSecret),
            "redirect_uri" to listOf(getRedirectUrl(organization.slug)),
          ),
        ),
        headers,
      )

    val response =
      restTemplate.exchange(
        "https://slack.com/api/oauth.v2.access",
        HttpMethod.POST,
        request,
        String::class.java,
      )

    val connectToSlackResponseString =
      response.body ?: let {
        val exception = BadRequestException(Message.SLACK_CONNECTION_FAILED)
        // this should not happen, but if it does, let's log it
        Sentry.captureException(exception)
        throw exception
      }

    val parsed = objectMapper.readValue<ConnectToSlackResponse>(connectToSlackResponseString)
    if (!parsed.error.isNullOrEmpty()) {
      throw BadRequestException(Message.SLACK_CONNECTION_ERROR, listOf(parsed.error))
    }

    if (parsed.access_token.isNullOrEmpty()) {
      throw BadRequestException(Message.SLACK_CONNECTION_ERROR)
    }

    return parsed
  }

  @Transactional
  fun connect(
    organization: Organization,
    connectToSlackResponse: ConnectToSlackResponse,
    author: UserAccount,
  ): OrganizationSlackWorkspace {
    val slackTeamId = connectToSlackResponse.team?.id ?: throw IllegalArgumentException("Team is null")
    if (organizationSlackWorkspaceRepository.findBySlackTeamId(slackTeamId) != null) {
      throw BadRequestException(Message.SLACK_WORKSPACE_ALREADY_CONNECTED)
    }

    val organizationSlackWorkspace = OrganizationSlackWorkspace()
    organizationSlackWorkspace.organization = organization
    organizationSlackWorkspace.author = author
    organizationSlackWorkspace.slackTeamId =
      slackTeamId
    organizationSlackWorkspace.slackTeamName =
      connectToSlackResponse.team.name
    organizationSlackWorkspace.accessToken =
      connectToSlackResponse.access_token ?: throw IllegalArgumentException("Access token is null")
    return organizationSlackWorkspaceRepository.save(organizationSlackWorkspace)
  }

  @Transactional
  fun connect(organizationSlackWorkspace: OrganizationSlackWorkspace): OrganizationSlackWorkspace {
    return organizationSlackWorkspaceRepository.save(organizationSlackWorkspace)
  }

  @Transactional
  fun disconnect(
    organizationId: Long,
    workspaceId: Long,
  ) {
    val organizationSlackWorkspace =
      organizationSlackWorkspaceRepository.findByOrganizationIdAndId(organizationId, workspaceId)
        ?: throw NotFoundException()
    delete(organizationSlackWorkspace)
    val uninstall =
      slackClient.methods().appsUninstall(
        AppsUninstallRequest
          .builder()
          .token(organizationSlackWorkspace.accessToken)
          .clientId(slackProperties.clientId)
          .clientSecret(slackProperties.clientSecret)
          .build(),
      )
    logAppUninstallError(organizationSlackWorkspace.id, organizationSlackWorkspace.organization.id, uninstall)
  }

  fun delete(organizationSlackWorkspace: OrganizationSlackWorkspace) {
    organizationSlackWorkspaceRepository.delete(organizationSlackWorkspace)
  }

  private fun logAppUninstallError(
    workspaceId: Long,
    organizationId: Long,
    uninstall: AppsUninstallResponse,
  ) {
    if (!uninstall.isOk) {
      val dummyException = RuntimeException("Error while uninstalling app from the client workspace.")
      Sentry.addBreadcrumb("WorkspaceId: $workspaceId")
      Sentry.addBreadcrumb("OrganizationId: $organizationId")
      Sentry.captureException(dummyException)
      logger.error(dummyException.message)
    }
  }

  @Transactional
  fun deleteWorkspaceWhenUninstalled(slackTeamId: String) {
    val workspace = findBySlackTeamId(slackTeamId) ?: return
    delete(workspace)
  }

  fun getRedirectUrl(organizationSlug: String): String {
    return "${frontendUrlProvider.url}/organizations/$organizationSlug/apps/slack-oauth2-success"
  }

  fun get(workspaceId: Long): OrganizationSlackWorkspace {
    return organizationSlackWorkspaceRepository.find(
      workspaceId,
    ) ?: throw NotFoundException(Message.SLACK_WORKSPACE_NOT_FOUND)
  }

  fun find(workspaceId: Long): OrganizationSlackWorkspace? {
    return organizationSlackWorkspaceRepository.find(workspaceId)
  }

  fun saveAll(organizationSlackWorkspaces: List<OrganizationSlackWorkspace>) {
    organizationSlackWorkspaceRepository.saveAll(organizationSlackWorkspaces)
  }
}
