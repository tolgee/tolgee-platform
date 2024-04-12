package io.tolgee.service.slackIntegration

import io.tolgee.configuration.tolgee.SlackProperties
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
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
  private val slackProperties: SlackProperties,
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
    val workspace = getWorkspace(slackConfigDto.slackTeamId)
    val slackConfig =
      SlackConfig(
        project = slackConfigDto.project,
        userAccount = slackConfigDto.userAccount,
        channelId = slackConfigDto.channelId,
      ).apply {
        onEvent = slackConfigDto.onEvent ?: EventName.ALL
        isGlobalSubscription = slackConfigDto.languageTag.isNullOrBlank()
        this.organizationSlackWorkspace = workspace
      }

    val existingConfigs = get(slackConfig.project.id, slackConfig.channelId)
    return if (existingConfigs == null) {
      slackConfigRepository.save(slackConfig)
      workspace?.slackSubscriptions?.add(slackConfig)

      if (!slackConfig.isGlobalSubscription) {
        addPreferenceToConfig(slackConfig, slackConfigDto.languageTag!!, slackConfigDto.onEvent ?: EventName.ALL)
      }
      automationService.createForSlackIntegration(slackConfig)
      slackConfig
    } else {
      update(slackConfigDto)
    }
  }

  fun getWorkspace(slackTeamId: String): OrganizationSlackWorkspace? {
    if (slackProperties.token != null) {
      return null
    }
    return organizationSlackWorkspaceService.findBySlackTeamId(slackTeamId)
      ?: throw SlackWorkspaceNotFound()
  }

  fun update(slackConfigDto: SlackConfigDto): SlackConfig {
    val slackConfig = get(slackConfigDto.project.id, slackConfigDto.channelId) ?: throw Exception()
    slackConfigDto.onEvent?.let { eventName ->
      slackConfig.onEvent = eventName
    }

    if (slackConfigDto.languageTag.isNullOrBlank()) {
      slackConfig.isGlobalSubscription = true
    } else {
      if (slackConfig.preferences.isEmpty() ||
        !slackConfig.preferences.any { it.languageTag == slackConfigDto.languageTag }
      ) {
        addPreferenceToConfig(slackConfig, slackConfigDto.languageTag, slackConfigDto.onEvent ?: EventName.ALL)
      } else {
        updatePreferenceInConfig(slackConfig, slackConfigDto.languageTag, slackConfigDto.onEvent ?: EventName.ALL)
      }
    }

    automationService.updateForSlackConfig(slackConfig)
    slackConfigRepository.save(slackConfig)
    return slackConfig
  }

  private fun updatePreferenceInConfig(
    slackConfig: SlackConfig,
    languageTag: String,
    eventName: EventName,
  ) {
    val pref = slackConfig.preferences.find { it.languageTag == languageTag } ?: return
    slackConfigPreferenceService.update(pref, eventName)
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
