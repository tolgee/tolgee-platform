package io.tolgee.service.slackIntegration

import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.repository.slackIntegration.SlackConfigRepository
import io.tolgee.service.automations.AutomationService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class SlackConfigService(
  private val automationService: AutomationService,
  private val slackConfigRepository: SlackConfigRepository,
  private val slackConfigPreferenceService: SlackConfigPreferenceService,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
) {
  fun get(
    projectId: Long,
    channelId: String,
  ): SlackConfig? {
    return slackConfigRepository.findByProjectIdAndChannelId(projectId, channelId)
  }

  fun get(configId: Long): SlackConfig {
    return slackConfigRepository.findById(configId).orElseThrow { NotFoundException() }
  }

  fun getAllByChannelId(channelId: String): List<SlackConfig> {
    return slackConfigRepository.getAllByChannelId(channelId)
  }

  @Transactional
  fun delete(
    projectId: Long,
    channelId: String,
  ): Boolean {
    try {
      val config = get(projectId, channelId) ?: return false
      automationService.deleteForSlackIntegration(config)
      slackConfigRepository.delete(config)
    } catch (e: NotFoundException) {
      return false
    }
    return true
  }

  @Transactional
  fun create(slackConfigDto: SlackConfigDto): SlackConfig {
    val orgToWorkspaceLink =
      organizationSlackWorkspaceService.findBySlackTeamId(slackConfigDto.slackTeamId)
        ?: throw SlackWorkspaceNotFound()

    val slackConfig =
      SlackConfig(
        project = slackConfigDto.project,
        userAccount = slackConfigDto.userAccount,
        channelId = slackConfigDto.channelId,
      ).apply {
        onEvent = slackConfigDto.onEvent ?: EventName.ALL
        isGlobalSubscription = slackConfigDto.languageTag.isNullOrBlank()
        this.organizationSlackWorkspace = orgToWorkspaceLink
      }

    val existingConfigs = get(slackConfig.project.id, slackConfig.channelId)
    return if (existingConfigs == null) {
      slackConfigRepository.save(slackConfig)
      orgToWorkspaceLink.slackSubscriptions.add(slackConfig)

      if (!slackConfig.isGlobalSubscription) {
        addPreferenceToConfig(slackConfig, slackConfigDto.languageTag!!, slackConfigDto.onEvent ?: EventName.ALL)
      }
      automationService.createForSlackIntegration(slackConfig)
      slackConfig
    } else {
      update(slackConfigDto)
    }
  }

  fun update(slackConfigDto: SlackConfigDto): SlackConfig {
    val slackConfig = get(slackConfigDto.project.id, slackConfigDto.channelId) ?: throw Exception()
    slackConfigDto.onEvent?.let { eventName ->
      slackConfig.onEvent = eventName
    }

    if (slackConfigDto.languageTag.isNullOrBlank()) {
      slackConfig.isGlobalSubscription = true
    } else {
      addPreferenceToConfig(slackConfig, slackConfigDto.languageTag, slackConfigDto.onEvent ?: EventName.ALL)
    }

    automationService.updateForSlackConfig(slackConfig)
    slackConfigRepository.save(slackConfig)
    return slackConfig
  }

  private fun addPreferenceToConfig(
    slackConfig: SlackConfig,
    langTag: String,
    onEvent: EventName,
  ) {
    val pref =
      slackConfigPreferenceService.create(
        slackConfig,
        langTag,
        onEvent,
      )
    slackConfig.preferences.add(pref)
  }
}
