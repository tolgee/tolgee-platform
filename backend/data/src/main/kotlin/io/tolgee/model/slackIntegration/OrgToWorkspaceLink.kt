package io.tolgee.model.slackIntegration

import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany

@Entity
class OrgToWorkspaceLink : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  var organization: Organization? = null
  var workSpace: String = ""
  var channelName: String = ""
  var author: String = ""
  var workSpaceName: String = ""

  @OneToMany(mappedBy = "orgToWorkspaceLink", fetch = FetchType.LAZY, orphanRemoval = true)
  var slackSubscription: MutableList<SlackConfig> = mutableListOf()
}
