package io.tolgee.model.automations

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
  indexes = [
    Index(columnList = "automation_id"),
  ],
)
class AutomationTrigger(
  @ManyToOne(fetch = FetchType.LAZY)
  var automation: Automation,
) : StandardAuditModel() {
  @Enumerated(STRING)
  var type: AutomationTriggerType = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION

  /**
   * only when type is ACTIVITY, if type is ACTIVITY and this is null, it means all activity types
   */
  @Enumerated(STRING)
  var activityType: ActivityType? = null

  var debounceDurationInMs: Long? = null
}
