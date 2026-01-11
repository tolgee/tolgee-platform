package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationActionType
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType

class WebhooksTestData : BaseTestData() {
  val webhookConfig =
    projectBuilder.addWebhookConfig {
      url = "https://this-will-hopefully-never-exist.com/wh"
      webhookSecret = "whsec_hello"
    }

  val automation =
    projectBuilder.addAutomation {
      this.triggers.add(
        AutomationTrigger(this)
          .also {
            it.type = AutomationTriggerType.ACTIVITY
            it.activityType = null
          },
      )
      this.actions.add(
        AutomationAction(this).also {
          it.type = AutomationActionType.WEBHOOK
          it.webhookConfig = webhookConfig.self
        },
      )
    }
}
