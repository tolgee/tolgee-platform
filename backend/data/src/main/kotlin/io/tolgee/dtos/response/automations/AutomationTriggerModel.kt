package io.tolgee.dtos.response.automations

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.automations.AutomationTriggerType

class AutomationTriggerModel(
  var id: Long,
  var type: AutomationTriggerType = AutomationTriggerType.MANUAL,
  var activityType: ActivityType? = null,
  var debounceDurationInMs: Long? = null
)
