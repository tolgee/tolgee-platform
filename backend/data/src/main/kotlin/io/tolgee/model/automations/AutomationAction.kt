package io.tolgee.model.automations

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne

@Entity
class AutomationAction(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
) : StandardAuditModel() {
  var type: AutomationActionType = AutomationActionType.CDN_PUBLISH
}
