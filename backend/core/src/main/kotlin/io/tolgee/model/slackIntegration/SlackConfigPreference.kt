package io.tolgee.model.slackIntegration

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "slack_config_id"),
  ],
)
class SlackConfigPreference(
  @ManyToOne
  var slackConfig: SlackConfig,
  var languageTag: String? = null,
) : StandardAuditModel() {
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var events: MutableSet<SlackEventType> = mutableSetOf()
}
