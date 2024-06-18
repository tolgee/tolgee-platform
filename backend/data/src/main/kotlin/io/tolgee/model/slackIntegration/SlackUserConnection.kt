package io.tolgee.model.slackIntegration

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.*

@Entity
@Table(
  uniqueConstraints = [UniqueConstraint(columnNames = ["user_account_id", "slack_team_id"])],
)
class SlackUserConnection : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var userAccount: UserAccount

  lateinit var slackUserId: String

  lateinit var slackTeamId: String
}
