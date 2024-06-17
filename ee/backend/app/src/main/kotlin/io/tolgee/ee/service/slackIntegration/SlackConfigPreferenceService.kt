package io.tolgee.ee.service.slackIntegration

import io.tolgee.ee.repository.slackIntegration.SlackConfigPreferenceRepository
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackConfigPreference
import io.tolgee.model.slackIntegration.SlackEventType
import org.springframework.stereotype.Service

@Service
class SlackConfigPreferenceService(
  private val slackConfigPreferenceRepository: SlackConfigPreferenceRepository,
) {
  fun create(
    slackConfig: SlackConfig,
    langTag: String,
    events: MutableSet<SlackEventType>,
  ): SlackConfigPreference {
    val preference =
      SlackConfigPreference(slackConfig, langTag).apply {
        this.events =
          if (events.isEmpty()) {
            mutableSetOf(SlackEventType.ALL)
          } else {
            events
          }
      }
    return slackConfigPreferenceRepository.save(preference)
  }

  fun update(
    slackConfigPreference: SlackConfigPreference,
    events: MutableSet<SlackEventType>,
  ): SlackConfigPreference {
    if (events.isNotEmpty()) {
      slackConfigPreference.events = events
    }
    return slackConfigPreferenceRepository.save(slackConfigPreference)
  }

  fun delete(slackConfigPreference: SlackConfigPreference) {
    slackConfigPreferenceRepository.delete(slackConfigPreference)
  }
}
