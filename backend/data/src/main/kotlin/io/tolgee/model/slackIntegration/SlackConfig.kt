package io.tolgee.model.slackIntegration

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne

@Entity
class SlackConfig(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project
): StandardAuditModel() {
}
