package io.tolgee.model.slackIntegration

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Type

@Entity
class SavedSlackMessage(
  val messageTs: String,
  @ManyToOne(fetch = FetchType.LAZY)
  val slackConfig: SlackConfig,
  val keyId: Long,
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var langTags: Set<String>,
) : StandardAuditModel()
