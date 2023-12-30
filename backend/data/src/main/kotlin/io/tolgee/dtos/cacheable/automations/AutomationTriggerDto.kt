package io.tolgee.dtos.cacheable.automations

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.automations.AutomationTriggerType

class AutomationTriggerDto(
  var id: Long,
  var type: AutomationTriggerType,
  var activityType: ActivityType? = null,
  var debounceDurationInMs: Long? = null,
)
