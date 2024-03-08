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

  @Transactional
  fun delete(
    projectId: Long,
    channelId: String,
  ) {
    val config = get(projectId, channelId) ?: return
    automationService.deleteForSlackIntegration(config)
    slackConfigRepository.delete(config)
  }

  fun create(slackConfigDto: SlackConfigDto): SlackConfig {
    val slackConfig =
      SlackConfig(
        project = slackConfigDto.project,
        userAccount = slackConfigDto.userAccount,
        channelId = slackConfigDto.channelId,
      ).apply {
        languageTags =
          if (!slackConfigDto.languageTag.isNullOrBlank()) {
            mutableSetOf(slackConfigDto.languageTag)
          } else {
            mutableSetOf()
          }
        visibilityOptions = visibilityOptions
        slackId = slackConfigDto.slackId
        onEvent = slackConfigDto.onEvent ?: EventName.ALL
        isGlobalSubscription = slackConfigDto.languageTag?.isEmpty() ?: true
      }

    val existingConfigs = get(slackConfig.project.id, slackConfig.channelId)
    return if (existingConfigs == null) {
      slackConfigRepository.save(slackConfig)
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

    slackConfigDto.languageTag.let { tag ->
      if (!tag.isNullOrBlank()) {
        slackConfig.languageTags.add(tag)
      } else {
        slackConfig.isGlobalSubscription = true
      }
    }

    automationService.updateForSlackConfig(slackConfig)
    slackConfigRepository.save(slackConfig)
    return slackConfig
  }
}
