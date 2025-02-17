package io.tolgee.ee.service.slackIntegration

import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.component.slackIntegration.slashcommand.SlackErrorProvider
import io.tolgee.ee.repository.slackIntegration.SlackConfigRepository
import io.tolgee.exceptions.SlackErrorException
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.service.automations.AutomationService
import io.tolgee.service.language.LanguageService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class SlackConfigManageService(
  private val automationService: AutomationService,
  private val slackConfigReadService: SlackConfigReadService,
  private val slackConfigRepository: SlackConfigRepository,
  private val slackConfigPreferenceService: SlackConfigPreferenceService,
  private val languageService: LanguageService,
  private val slackErrorProvider: SlackErrorProvider,
) {
  @Transactional
  fun delete(
    projectId: Long,
    channelId: String,
    languageTag: String,
  ) {
    val config =
      slackConfigReadService.find(projectId, channelId)
        ?: throw SlackErrorException(slackErrorProvider.getNotSubscribedYetError())

    if (languageTag.isNotEmpty()) {
      val pref =
        config.preferences.find {
          it.languageTag == languageTag
        } ?: throw SlackErrorException(slackErrorProvider.getNotSubscribedToLanguageOrBadLanguageTagError())
      slackConfigPreferenceService.delete(pref)
    } else {
      automationService.deleteForSlackIntegration(config)
      slackConfigRepository.delete(config)
    }
  }

  @Transactional
  fun createOrUpdate(slackConfigDto: SlackConfigDto): SlackConfig {
    val existingConfig =
      slackConfigReadService.find(slackConfigDto.project.id, slackConfigDto.channelId)
        ?: return create(slackConfigDto)
    return update(existingConfig, slackConfigDto)
  }

  private fun create(dto: SlackConfigDto): SlackConfig {
    if (dto.isGlobal == false && dto.languageTag.isNullOrBlank()) {
      throw SlackErrorException(slackErrorProvider.getInvalidGlobalSubscriptionError())
    }

    val workspace = slackConfigReadService.findWorkspace(dto.slackTeamId)
    val slackConfig =
      SlackConfig(
        project = dto.project,
        userAccount = dto.userAccount,
        channelId = dto.channelId,
      ).apply {
        events =
          if (dto.events.isEmpty()) {
            mutableSetOf(SlackEventType.ALL)
          } else {
            dto.events
          }
        isGlobalSubscription = dto.isGlobal ?: dto.languageTag.isNullOrBlank()
        this.organizationSlackWorkspace = workspace
      }
    slackConfigRepository.save(slackConfig)
    workspace?.slackSubscriptions?.add(slackConfig)

    if (!slackConfig.isGlobalSubscription) {
      addPreferenceToConfig(slackConfig, dto.languageTag!!, events = dto.events)
    }
    automationService.createForSlackIntegration(slackConfig)
    return slackConfig
  }

  fun update(
    slackConfig: SlackConfig,
    dto: SlackConfigDto,
  ): SlackConfig {
    val workspace = slackConfigReadService.findWorkspace(dto.slackTeamId)

    if (dto.events.isNotEmpty()) {
      if (dto.languageTag.isNullOrEmpty()) {
        slackConfig.events = dto.events
      }
    }

    dto.isGlobal?.let {
      slackConfig.isGlobalSubscription = it

      if (!slackConfig.isGlobalSubscription &&
        dto.languageTag.isNullOrBlank() &&
        slackConfig.preferences.isEmpty()
      ) {
        throw SlackErrorException(slackErrorProvider.getInvalidGlobalSubscriptionError())
      }
    }

    if (dto.languageTag.isNullOrBlank()) {
      if (dto.isGlobal != null && dto.isGlobal != false) {
        slackConfig.isGlobalSubscription = true
      }
    } else {
      if (slackConfig.preferences.isEmpty() ||
        !slackConfig.preferences.any { it.languageTag == dto.languageTag }
      ) {
        addPreferenceToConfig(slackConfig, dto.languageTag!!, events = dto.events)
      } else {
        updatePreferenceInConfig(slackConfig, dto.languageTag!!, events = dto.events)
      }
    }

    slackConfig.organizationSlackWorkspace = workspace
    automationService.updateForSlackConfig(slackConfig)
    slackConfigRepository.save(slackConfig)
    return slackConfig
  }

  private fun updatePreferenceInConfig(
    slackConfig: SlackConfig,
    languageTag: String,
    events: MutableSet<SlackEventType>,
  ) {
    val pref = slackConfig.preferences.find { it.languageTag == languageTag } ?: return
    slackConfigPreferenceService.update(pref, events)
  }

  private fun addPreferenceToConfig(
    slackConfig: SlackConfig,
    langTag: String,
    events: MutableSet<SlackEventType>,
  ) {
    languageService.findByTag(
      langTag,
      slackConfig.project,
    ) ?: throw SlackErrorException(slackErrorProvider.getInvalidLangTagError())

    val pref =
      slackConfigPreferenceService.create(
        slackConfig,
        langTag,
        events,
      )
    slackConfig.preferences.add(pref)
  }
}
