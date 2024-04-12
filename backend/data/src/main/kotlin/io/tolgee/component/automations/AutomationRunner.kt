package io.tolgee.component.automations

import io.tolgee.service.automations.AutomationService
import io.tolgee.util.Logging
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class AutomationRunner(
  private val automationService: AutomationService,
  private val applicationContext: ApplicationContext,
) : Logging {
  fun run(
    actionId: Long,
    activityRevisionId: Long?,
  ) {
    val action = automationService.getAction(actionId)
    applicationContext.getBean(action.type.processor.java).process(action, activityRevisionId)
  }
}
