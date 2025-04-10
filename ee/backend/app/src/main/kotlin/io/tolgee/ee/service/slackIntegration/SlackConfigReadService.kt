package io.tolgee.ee.service.slackIntegration

import io.tolgee.configuration.tolgee.SlackProperties
import io.tolgee.ee.repository.slackIntegration.SlackConfigRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SlackConfig
import org.springframework.stereotype.Service

@Service
class SlackConfigReadService(
  private val slackConfigRepository: SlackConfigRepository,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
  private val slackProperties: SlackProperties,
) {
  fun find(
    projectId: Long,
    channelId: String,
  ): SlackConfig? = slackConfigRepository.findByProjectIdAndChannelId(projectId, channelId)

  fun get(configId: Long): SlackConfig = slackConfigRepository.findById(configId).orElseThrow { NotFoundException() }

  fun getAllByChannelId(channelId: String): List<SlackConfig> = slackConfigRepository.getAllByChannelId(channelId)

  fun findAll(): List<SlackConfig> = slackConfigRepository.findAll()

  fun findWorkspace(slackTeamId: String): OrganizationSlackWorkspace? {
    if (slackProperties.token != null) {
      return null
    }
    return organizationSlackWorkspaceService.findBySlackTeamId(slackTeamId)
      ?: throw SlackWorkspaceNotFound()
  }
}
