package io.tolgee.model.slackIntegration

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Type

@Entity
class SlackConfigPreference(
  @ManyToOne
  var slackConfig: SlackConfig,
  var languageTag: String? = null,
) : StandardAuditModel() {
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var events: MutableSet<SlackEventType> = mutableSetOf()
}
