package io.tolgee.ee.service.slackIntegration

import io.tolgee.model.slackIntegration.EventName
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackConfigPreference
import io.tolgee.repository.slackIntegration.SlackConfigPreferenceRepository
import org.springframework.stereotype.Service

@Service
class SlackConfigPreferenceService(
  private val slackConfigPreferenceRepository: SlackConfigPreferenceRepository,
) {
  fun create(
    slackConfig: SlackConfig,
    langTag: String,
    eventName: EventName,
  ): SlackConfigPreference {
    return slackConfigPreferenceRepository.save(SlackConfigPreference(slackConfig, langTag, eventName))
  }

  fun update(
    slackConfigPreference: SlackConfigPreference,
    eventName: EventName,
  ): SlackConfigPreference {
    slackConfigPreference.onEvent = eventName
    return slackConfigPreferenceRepository.save(slackConfigPreference)
  }

  fun get(id: Long) {
  }

  fun delete(slackConfigPreference: SlackConfigPreference) {
    delete(slackConfigPreference)
  }
}
