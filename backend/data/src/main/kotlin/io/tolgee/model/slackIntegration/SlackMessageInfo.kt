package io.tolgee.model.slackIntegration

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.ColumnDefault

@Entity
class SlackMessageInfo(
  @ManyToOne()
  var slackMessage: SavedSlackMessage = SavedSlackMessage(),
  var langTag: String = "",
) : StandardAuditModel() {
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'GLOBAL'")
  var subscriptionType: SubscriptionType = SubscriptionType.GLOBAL

  var authorContext: String = "" // string containing author name, event and date
}
