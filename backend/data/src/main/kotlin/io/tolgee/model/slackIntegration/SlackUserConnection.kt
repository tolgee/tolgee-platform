package io.tolgee.model.slackIntegration

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["slack_user_id", "user_account_id"])])
class SlackUserConnection : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var userAccount: UserAccount

  lateinit var slackUserId: String
}
