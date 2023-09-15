package io.tolgee.dtos.request.automation

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.automations.AutomationTriggerType

data class AutomationTriggerRequest(
  var type: AutomationTriggerType,
  var activityType: ActivityType?,
  var debounceDurationInMs: Long?
)
