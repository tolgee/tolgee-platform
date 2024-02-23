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
  private val slackConfigRepository: SlackConfigRepository
) {

  fun get(
    projectId: Long,
    channelId: String,
  ): SlackConfig? {
    return slackConfigRepository.findByProjectIdAndChannelId(projectId, channelId)
  }

  fun get(
    configId: Long,
  ): SlackConfig {
    return slackConfigRepository.findById(configId).orElseThrow { NotFoundException() }
  }

  @Transactional
  fun delete(
    projectId: Long,
    channelId: String
  ) {
    val config = get(projectId, channelId) ?: return
    automationService.deleteForSlackIntegration(config)
    slackConfigRepository.delete(config)
  }

  fun create(
    slackConfigDto: SlackConfigDto
  ): SlackConfig {
    val slackConfig = SlackConfig(
      project = slackConfigDto.project,
      userAccount = slackConfigDto.userAccount,
      channelId = slackConfigDto.channelId,
    ).apply {
      languageTag = slackConfigDto.languageTag
      visibilityOptions = visibilityOptions
      slackId = slackConfigDto.slackId
      onEvent = slackConfigDto.onEvent ?: EventName.ALL
    }

    slackConfigRepository.save(slackConfig)
    automationService.createForSlackIntegration(slackConfig)
    return slackConfig
  }

  fun update(
    slackConfigDto: SlackConfigDto
  ): SlackConfig {
    val slackConfig = get(slackConfigDto.project.id, slackConfigDto.channelId)!!

    slackConfigDto.languageTag?.let { tag ->
      slackConfig.languageTag = tag
    }

    slackConfigDto.visibilityOptions?.let { visibilityOptions ->
      slackConfig.visibilityOptions = visibilityOptions
    }

    slackConfigDto.onEvent?.let { eventName ->
      slackConfig.onEvent = eventName
    }

    automationService.updateForSlackConfig(slackConfig)
    slackConfigRepository.save(slackConfig)
    return slackConfig
  }

  fun ifExist(projectId: Long, channelId: String) = get(projectId, channelId) != null

}
