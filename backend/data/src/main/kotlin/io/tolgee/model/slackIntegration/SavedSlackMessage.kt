package io.tolgee.model.slackIntegration

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne

@Entity
class SavedSlackMessage(
  val messageTs: String,
  @ManyToOne(fetch = FetchType.LAZY)
  val slackConfig: SlackConfig,
  val keyId: Long,
  @ElementCollection
  val langTags: Set<String>,
): StandardAuditModel() {

}
