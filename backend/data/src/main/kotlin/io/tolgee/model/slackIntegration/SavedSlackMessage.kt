package io.tolgee.model.slackIntegration

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "slack_config_id"),
  ],
)
class SavedSlackMessage(
  val messageTimestamp: String = "",
  @ManyToOne(fetch = FetchType.LAZY)
  var slackConfig: SlackConfig,
  var keyId: Long = 0L,
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var languageTags: Set<String> = setOf(),
  var createdKeyBlocks: Boolean = false,
) : StandardAuditModel() {
  @OneToMany(mappedBy = "slackMessage", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  var info: MutableList<SlackMessageInfo> = mutableListOf()
}
