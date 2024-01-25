package io.tolgee.service.slackIntegration

import io.tolgee.model.Project
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.repository.slackIntegration.SlackConfigRepository
import io.tolgee.service.automations.AutomationService
import org.springframework.stereotype.Service

@Service
class SlackConfigService(
  private val automationService: AutomationService,
  private val slackConfigRepository: SlackConfigRepository
) {

  fun create(
    project: Project,
  ): SlackConfig {
    val slackConfig = SlackConfig(project)
    slackConfigRepository.save(slackConfig)
    automationService.createForSlackIntegration(slackConfig)
    return slackConfig
  }

}
