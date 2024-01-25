package io.tolgee.model.slackIntegration

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.automations.AutomationAction
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany

@Entity
class SlackConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project
): StandardAuditModel() {

  @OneToMany(mappedBy = "slackConfig", orphanRemoval = true)
  var automationActions: MutableList<AutomationAction> = mutableListOf()
}
