package io.tolgee.model.slackIntegration

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault

@Entity
@Table(
  indexes = [
    Index(columnList = "slack_message_id"),
  ],
)
class SlackMessageInfo(
  @ManyToOne(fetch = FetchType.LAZY)
  var slackMessage: SavedSlackMessage,
  var languageTag: String = "",
) : StandardAuditModel() {
  @Enumerated(EnumType.STRING)
  @ColumnDefault("GLOBAL")
  var subscriptionType: SubscriptionType = SubscriptionType.GLOBAL

  var authorContext: String = "" // string containing author name, event and date
}
