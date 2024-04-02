package io.tolgee.model.slackIntegration

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.ColumnDefault

@Entity
class SlackConfigPreference(
  @ManyToOne
  var slackConfig: SlackConfig,
  var languageTag: String? = null,
  @ColumnDefault("0")
  var onEvent: EventName = EventName.ALL,
) : StandardAuditModel()
