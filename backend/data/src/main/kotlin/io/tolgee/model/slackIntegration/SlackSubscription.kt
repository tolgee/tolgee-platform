package io.tolgee.model.slackIntegration

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne

@Entity
class SlackSubscription : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  var userAccount: UserAccount? = null
  var slackNickName: String = ""
  var slackUserId: String = ""
}
