package io.tolgee.hateoas.automation

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.automations.AutomationTriggerType

class AutomationTriggerModel(
  var id: Long,
  var type: AutomationTriggerType = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION,
  var activityType: ActivityType? = null,
  var debounceDurationInMs: Long? = null,
)
