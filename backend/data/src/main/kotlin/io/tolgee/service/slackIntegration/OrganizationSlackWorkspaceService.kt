package io.tolgee.service.slackIntegration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.sentry.Sentry
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.configuration.tolgee.SlackProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.ConnectToSlackDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.repository.slackIntegration.OrganizationSlackWorkspaceRepository
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
) {
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
    val organizationSlackWorkspace = OrganizationSlackWorkspace()
    organizationSlackWorkspace.organization = organization
    organizationSlackWorkspace.author = author
    organizationSlackWorkspace.slackTeamId =
      connectToSlackResponse.team?.id ?: throw IllegalArgumentException("Team is null")
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
    organizationSlackWorkspaceRepository.delete(organizationSlackWorkspace)
  }

  fun getRedirectUrl(organizationSlug: String): String {
    return "${frontendUrlProvider.url}/organizations/$organizationSlug/slack/oauth2-success"
  }
}
