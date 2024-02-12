package io.tolgee.model.slackIntegration

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.automations.AutomationAction
import jakarta.persistence.*

@Entity
class SlackConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,

  @OneToOne(fetch = FetchType.LAZY)
  var userAccount: UserAccount
): StandardAuditModel() {

  @OneToMany(mappedBy = "slackConfig", orphanRemoval = true)
  var automationActions: MutableList<AutomationAction> = mutableListOf()

  var channelId: String? = ""
}
