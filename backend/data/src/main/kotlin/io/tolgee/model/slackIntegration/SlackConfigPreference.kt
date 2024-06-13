package io.tolgee.model.slackIntegration

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type

@Entity
class SlackConfigPreference(
  @ManyToOne
  var slackConfig: SlackConfig,
  var languageTag: String? = null,
) : StandardAuditModel() {
  @ColumnDefault("0")
  @Enumerated(EnumType.STRING)
  var onEvent: EventName = EventName.ALL

  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var events: MutableSet<EventName> = mutableSetOf()
}
